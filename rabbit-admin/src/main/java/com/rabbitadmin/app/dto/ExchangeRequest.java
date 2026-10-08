package com.rabbitadmin.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,100}$",
                message = "Solo letras, numeros, punto, guion y guion bajo (max 100)")
        String name,
        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "^(direct|topic|fanout|headers)$",
                message = "El tipo debe ser direct, topic, fanout o headers")
        String type,
        Boolean durable
) {}