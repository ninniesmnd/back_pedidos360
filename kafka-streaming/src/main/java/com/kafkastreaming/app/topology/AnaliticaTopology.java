package com.kafkastreaming.app.topology;

import com.kafkastreaming.app.config.StreamingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrada (negocio.eventos, key = localId, JSON de pedidos y ventas) -> salida (analitica.salida).
 * Los eventos se distinguen por sus campos: ventaId = venta, stockActual = stock bajo, tipoEvento = pedido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnaliticaTopology {

    private enum Tipo { PEDIDO, VENTA, STOCK, DESCONOCIDO }

    private final StreamingProperties props;
    private final JsonMapper mapper;

    public Topology construir() {
        StreamsBuilder builder = new StreamsBuilder();
        Duration ventana = Duration.ofSeconds(props.ventanaSegundos());

        KStream<String, String> entrada = builder
                .stream(props.entrada(), Consumed.with(Serdes.String(), Serdes.String()))
                .filter((k, v) -> k != null && v != null);

        // 1) Total vendido por local y ventana
        entrada.filter((k, v) -> tipo(v) == Tipo.VENTA)
                .mapValues(v -> numero(leer(v), "total"))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Double()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(ventana))
                .aggregate(() -> 0.0, (k, v, acc) -> acc + v,
                        Materialized.<String, Double, WindowStore<Bytes, byte[]>>as("ventas-por-local")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.Double()))
                .toStream()
                .map((wk, total) -> KeyValue.pair(wk.key(),
                        resultado("VENTAS_TOTAL", wk.key(), "total", total, wk.window())))
                .to(props.salida(), Produced.with(Serdes.String(), Serdes.String()));

        // 2) Cantidad de eventos de pedido por local, estado y ventana
        entrada.filter((k, v) -> tipo(v) == Tipo.PEDIDO)
                .map((k, v) -> KeyValue.pair(k + "|" + texto(leer(v), "estado"), v))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(ventana))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("pedidos-por-estado")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()))
                .toStream()
                .map((wk, n) -> {
                    String[] partes = wk.key().split("\\|", 2);
                    return KeyValue.pair(partes[0],
                            resultado("PEDIDOS_POR_ESTADO", partes[0], partes[1], n.doubleValue(), wk.window()));
                })
                .to(props.salida(), Produced.with(Serdes.String(), Serdes.String()));

        // 3) Alertas de stock bajo: pasan directo a la salida
        entrada.filter((k, v) -> tipo(v) == Tipo.STOCK)
                .map((k, v) -> {
                    Map<String, Object> m = leer(v);
                    return KeyValue.pair(k, escribir(armar("STOCK_BAJO", k, texto(m, "nombreProducto"),
                            numero(m, "stockActual"), null, null)));
                })
                .to(props.salida(), Produced.with(Serdes.String(), Serdes.String()));

        return builder.build();
    }

    // ---------- helpers ----------

    private Tipo tipo(String json) {
        Map<String, Object> m = leer(json);
        if (m.containsKey("ventaId")) return Tipo.VENTA;
        if (m.containsKey("stockActual")) return Tipo.STOCK;
        if (m.containsKey("tipoEvento")) return Tipo.PEDIDO;
        return Tipo.DESCONOCIDO;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leer(String json) {
        try {
            Map<String, Object> m = mapper.readValue(json, Map.class);
            return m != null ? m : Map.of();
        } catch (Exception ex) {
            log.warn("Evento descartado (JSON invalido): {}", ex.getMessage());
            return Map.of();
        }
    }

    private Double numero(Map<String, Object> m, String campo) {
        Object v = m.get(campo);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private String texto(Map<String, Object> m, String campo) {
        Object v = m.get(campo);
        return v == null ? "DESCONOCIDO" : String.valueOf(v);
    }

    private String resultado(String tipo, String localId, String clave, Double valor, Window ventana) {
        Instant ini = ventana.startTime();
        Instant fin = ventana.endTime();
        return escribir(armar(tipo, localId, clave, valor, ini, fin));
    }

    private Map<String, Object> armar(String tipo, String localId, String clave, Double valor,
                                      Instant ini, Instant fin) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tipo", tipo);
        long idLocal = 0L;
        try {
            idLocal = Long.parseLong(localId);
        } catch (NumberFormatException ignored) {
        }
        m.put("localId", idLocal);
        m.put("clave", clave);
        m.put("valor", valor);
        m.put("ventanaInicio", ini == null ? null : ini.toString());
        m.put("ventanaFin", fin == null ? null : fin.toString());
        m.put("generadoEn", Instant.now().toString());
        return m;
    }

    private String escribir(Map<String, Object> m) {
        return mapper.writeValueAsString(m);
    }
}
