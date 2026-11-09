package com.kafkaadmin.app.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mensaje que publica kafka-streaming en analitica.salida. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResultadoEvent(
        String tipo,
        Long localId,
        String clave,
        Double valor,
        String ventanaInicio,
        String ventanaFin,
        String generadoEn
) {}
