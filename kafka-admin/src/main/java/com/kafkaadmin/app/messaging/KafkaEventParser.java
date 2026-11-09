package com.kafkaadmin.app.messaging;

import com.kafkaadmin.app.messaging.error.MensajeInvalidoException;
import com.kafkaadmin.app.messaging.event.ResultadoEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaEventParser {

    public static final String HEADER_TIPO = "tipoEvento";

    private final JsonMapper jsonMapper;

    /**
     * Valida que el registro de negocio.eventos sea JSON valido y contenga un tipo de evento reconocible
     * (por header o por campos del payload). Si es invalido, lanza MensajeInvalidoException para enviarlo a DLT.
     */
    @SuppressWarnings("unchecked")
    public String validarEventoNegocio(ConsumerRecord<String, String> record) {
        String json = record.value();
        if (json == null || json.isBlank()) {
            throw new MensajeInvalidoException("Payload vacio");
        }
        Map<String, Object> mapa;
        try {
            mapa = jsonMapper.readValue(json, Map.class);
        } catch (JacksonException ex) {
            throw new MensajeInvalidoException("JSON invalido en evento de negocio: " + ex.getMessage());
        }
        if (mapa == null || mapa.isEmpty()) {
            throw new MensajeInvalidoException("Objeto JSON vacio");
        }

        Header h = record.headers().lastHeader(HEADER_TIPO);
        if (h != null && h.value() != null && h.value().length > 0) {
            return new String(h.value(), StandardCharsets.UTF_8);
        }
        Object tipoEvento = mapa.get("tipoEvento");
        if (tipoEvento != null && !String.valueOf(tipoEvento).isBlank()) {
            return String.valueOf(tipoEvento);
        }
        if (mapa.containsKey("ventaId") && mapa.get("localId") != null && mapa.get("total") != null) {
            return "VENTA_REGISTRADA";
        }
        if (mapa.containsKey("stockActual") && mapa.get("productoId") != null && mapa.get("localId") != null) {
            return "STOCK_BAJO";
        }
        throw new MensajeInvalidoException("Evento de negocio no reconocido o incompleto");
    }

    public ResultadoEvent resultado(String json) {
        if (json == null || json.isBlank()) {
            throw new MensajeInvalidoException("Payload vacio");
        }
        ResultadoEvent e;
        try {
            e = jsonMapper.readValue(json, ResultadoEvent.class);
        } catch (JacksonException ex) {
            throw new MensajeInvalidoException("JSON invalido para ResultadoEvent: " + ex.getMessage());
        }
        if (e == null || e.tipo() == null || e.tipo().isBlank() || e.localId() == null || e.valor() == null) {
            throw new MensajeInvalidoException("ResultadoEvent incompleto (tipo, localId, valor)");
        }
        return e;
    }
}
