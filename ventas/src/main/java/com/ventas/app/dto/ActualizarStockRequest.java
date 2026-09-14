package com.ventas.app.dto;

import jakarta.validation.constraints.NotNull;

// delta negativo = rebajar (venta/consumo), positivo = reponer stock.
public record ActualizarStockRequest(
        @NotNull Integer delta
) {}