package com.kafkastreaming.app;

import com.kafkastreaming.app.config.StreamingProperties;
import com.kafkastreaming.app.topology.AnaliticaTopology;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AnaliticaTopologyTests {

    private final JsonMapper mapper = JsonMapper.shared();

    @Test
    @SuppressWarnings("unchecked")
    void procesaVentasPedidosYAlertasStockSinCaerConMensajesMalformados() {
        StreamingProperties props = new StreamingProperties(
                "localhost:9092", "pedidos360-streaming-test",
                "negocio.eventos", "analitica.salida", 1, 1, 60L, 1000L
        );
        AnaliticaTopology analiticaTopology = new AnaliticaTopology(props, mapper);

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, props.applicationId());
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        try (TopologyTestDriver driver = new TopologyTestDriver(analiticaTopology.construir(), config)) {
            TestInputTopic<String, String> input = driver.createInputTopic(
                    "negocio.eventos", Serdes.String().serializer(), Serdes.String().serializer());
            TestOutputTopic<String, String> output = driver.createOutputTopic(
                    "analitica.salida", Serdes.String().deserializer(), Serdes.String().deserializer());

            Instant t0 = Instant.parse("2026-11-09T12:00:10Z");

            // 1. Mensaje malformado: debe descartarse sin interrumpir el stream
            input.pipeInput("1", "json-corrupto-invalido", t0);

            // 2. Dos ventas del local 1 en la misma ventana de 60s
            input.pipeInput("1", "{\"ventaId\":100,\"localId\":1,\"total\":10000.0}", t0.plusSeconds(1));
            input.pipeInput("1", "{\"ventaId\":101,\"localId\":1,\"total\":5500.0}", t0.plusSeconds(5));

            // 3. Dos eventos de pedido del local 1 en estado RECIBIDO
            input.pipeInput("1", "{\"tipoEvento\":\"PEDIDO_CREADO\",\"pedidoId\":10,\"localId\":1,\"estado\":\"RECIBIDO\"}", t0.plusSeconds(10));
            input.pipeInput("1", "{\"tipoEvento\":\"PEDIDO_CREADO\",\"pedidoId\":11,\"localId\":1,\"estado\":\"RECIBIDO\"}", t0.plusSeconds(15));

            // 4. Alerta de stock bajo en local 2
            input.pipeInput("2", "{\"productoId\":201,\"localId\":2,\"nombreProducto\":\"Bebida Zero\",\"stockActual\":3,\"umbral\":5}", t0.plusSeconds(20));

            List<KeyValue<String, String>> resultados = output.readKeyValuesToList();
            assertThat(resultados).isNotEmpty();

            // Verificar que la suma acumulada de ventas del local 1 llego a 15500.0
            List<Map<String, Object>> parsed = resultados.stream()
                    .map(kv -> (Map<String, Object>) mapper.readValue(kv.value, Map.class))
                    .toList();

            assertThat(parsed).anySatisfy(m -> {
                assertThat(m.get("tipo")).isEqualTo("VENTAS_TOTAL");
                assertThat(((Number) m.get("localId")).longValue()).isEqualTo(1L);
                assertThat(((Number) m.get("valor")).doubleValue()).isEqualTo(15500.0);
            });

            assertThat(parsed).anySatisfy(m -> {
                assertThat(m.get("tipo")).isEqualTo("PEDIDOS_POR_ESTADO");
                assertThat(((Number) m.get("localId")).longValue()).isEqualTo(1L);
                assertThat(m.get("clave")).isEqualTo("RECIBIDO");
                assertThat(((Number) m.get("valor")).doubleValue()).isEqualTo(2.0);
            });

            assertThat(parsed).anySatisfy(m -> {
                assertThat(m.get("tipo")).isEqualTo("STOCK_BAJO");
                assertThat(((Number) m.get("localId")).longValue()).isEqualTo(2L);
                assertThat(m.get("clave")).isEqualTo("Bebida Zero");
                assertThat(((Number) m.get("valor")).doubleValue()).isEqualTo(3.0);
            });
        }
    }
}
