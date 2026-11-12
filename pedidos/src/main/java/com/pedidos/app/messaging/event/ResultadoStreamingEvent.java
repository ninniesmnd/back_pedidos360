package com.pedidos.app.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Resultado procesado por kafka-streaming y publicado en analitica.salida. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResultadoStreamingEvent(
        String tipo,
        Long localId,
        String clave,
        Double valor,
        String ventanaInicio,
        String ventanaFin,
        String generadoEn
) {}
