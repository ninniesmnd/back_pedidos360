package com.ventas.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CrearProductoRequest(
        @NotNull Long localId,
        @NotNull String nombre,
        String descripcion,
        @NotNull @Positive Double precio,
        @NotNull @PositiveOrZero Integer stock,
        String categoria
) {}