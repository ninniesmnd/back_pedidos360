package com.rabbitadmin.app.exception;

public class RecursoProtegidoException extends RuntimeException {
    public RecursoProtegidoException(String nombre) {
        super("'" + nombre + "' es parte de la topologia del sistema y no se puede modificar");
    }
}