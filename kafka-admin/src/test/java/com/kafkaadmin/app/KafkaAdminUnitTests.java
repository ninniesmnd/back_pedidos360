package com.kafkaadmin.app;

import com.kafkaadmin.app.config.KafkaTopicConfig;
import com.kafkaadmin.app.config.KafkaTopicsProperties;
import com.kafkaadmin.app.messaging.KafkaEventParser;
import com.kafkaadmin.app.messaging.error.MensajeInvalidoException;
import com.kafkaadmin.app.messaging.event.ResultadoEvent;
import com.kafkaadmin.app.messaging.resultados.ResultadosService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaAdminUnitTests {

    private final KafkaEventParser parser = new KafkaEventParser(JsonMapper.shared());

    @Test
    void topicConfigDeclaraTresParticionesYTresReplicas() {
        KafkaTopicsProperties props = new KafkaTopicsProperties(
                3, 2,
                new KafkaTopicsProperties.Topic("negocio.eventos", 3, 604800000L, "delete"),
                new KafkaTopicsProperties.Topic("analitica.salida", 3, 2592000000L, "delete"),
                new KafkaTopicsProperties.Dlt(1, 1209600000L),
                new KafkaTopicsProperties.Retry(1000L, 2)
        );
        KafkaTopicConfig config = new KafkaTopicConfig(props);

        var entrada = config.entradaTopic();
        assertThat(entrada.name()).isEqualTo("negocio.eventos");
        assertThat(entrada.numPartitions()).isEqualTo(3);
        assertThat(entrada.replicationFactor()).isEqualTo((short) 3);
        assertThat(entrada.configs()).containsEntry(TopicConfig.RETENTION_MS_CONFIG, "604800000");
        assertThat(entrada.configs()).containsEntry(TopicConfig.CLEANUP_POLICY_CONFIG, "delete");
        assertThat(entrada.configs()).containsEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2");

        var salida = config.salidaTopic();
        assertThat(salida.name()).isEqualTo("analitica.salida");
        assertThat(salida.numPartitions()).isEqualTo(3);
        assertThat(salida.replicationFactor()).isEqualTo((short) 3);
    }

    @Test
    void parserValidaResultadosCorrectosYRechazaMalformados() {
        String jsonValido = """
                {"tipo":"VENTAS_TOTAL","localId":1,"clave":"total","valor":15000.0,"ventanaInicio":"2026-11-09T10:00:00Z","ventanaFin":"2026-11-09T10:01:00Z","generadoEn":"2026-11-09T10:00:05Z"}
                """;
        ResultadoEvent ev = parser.resultado(jsonValido);
        assertThat(ev.tipo()).isEqualTo("VENTAS_TOTAL");
        assertThat(ev.localId()).isEqualTo(1L);
        assertThat(ev.valor()).isEqualTo(15000.0);

        assertThatThrownBy(() -> parser.resultado("no-es-json"))
                .isInstanceOf(MensajeInvalidoException.class);
        assertThatThrownBy(() -> parser.resultado("{\"tipo\":\"VENTAS_TOTAL\"}"))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    @Test
    void parserReconoceEventosNegocioConOSinHeader() {
        ConsumerRecord<String, String> ventaSinHeader = new ConsumerRecord<>(
                "negocio.eventos", 0, 1L, "1",
                "{\"ventaId\":10,\"localId\":1,\"total\":8500.0}");
        assertThat(parser.validarEventoNegocio(ventaSinHeader)).isEqualTo("VENTA_REGISTRADA");

        ConsumerRecord<String, String> pedidoConHeader = new ConsumerRecord<>(
                "negocio.eventos", 0, 2L, "1",
                "{\"tipoEvento\":\"PEDIDO_CREADO\",\"pedidoId\":5,\"localId\":1}");
        pedidoConHeader.headers().add("tipoEvento", "PEDIDO_CREADO".getBytes(StandardCharsets.UTF_8));
        assertThat(parser.validarEventoNegocio(pedidoConHeader)).isEqualTo("PEDIDO_CREADO");

        ConsumerRecord<String, String> corrupto = new ConsumerRecord<>(
                "negocio.eventos", 0, 3L, "1", "basura");
        assertThatThrownBy(() -> parser.validarEventoNegocio(corrupto))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    @Test
    void resultadosServiceSeparaAlertasDeAgregaciones() {
        ResultadosService service = new ResultadosService();
        service.registrar(new ResultadoEvent("STOCK_BAJO", 1L, "Hamburguesa", 3.0, null, null, "2026-11-09T10:00:00Z"));
        service.registrar(new ResultadoEvent("VENTAS_TOTAL", 1L, "total", 25000.0, "2026-11-09T10:00:00Z", "2026-11-09T10:01:00Z", "2026-11-09T10:00:10Z"));

        assertThat(service.alertas()).hasSize(1);
        assertThat(service.alertas().get(0).clave()).isEqualTo("Hamburguesa");
        assertThat(service.resultados()).hasSize(1);
        assertThat(service.resultados().get(0).valor()).isEqualTo(25000.0);
    }
}
