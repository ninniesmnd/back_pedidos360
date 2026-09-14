package com.ventas.app.dto;

import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoProductoRequest(
        @NotNull Boolean activo
) {}