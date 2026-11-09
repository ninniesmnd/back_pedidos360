package com.kafkaadmin.app.messaging.error;

public class MensajeInvalidoException extends RuntimeException {
    public MensajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
