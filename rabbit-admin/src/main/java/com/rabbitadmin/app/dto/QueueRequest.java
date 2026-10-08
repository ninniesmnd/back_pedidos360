package com.rabbitadmin.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,100}$",
                message = "Solo letras, numeros, punto, guion y guion bajo (max 100)")
        String name,
        Boolean durable,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {}