package com.ventas.app.messaging.error;

public class MensajeNoProcesableException extends RuntimeException {
    public MensajeNoProcesableException(String mensaje) {
        super(mensaje);
    }
}