package com.ventas.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CrearVentaRequest(
        @NotNull Long localId,
        Long pedidoId,
        @NotEmpty @Valid List<ItemVentaRequest> items
) {
    public record ItemVentaRequest(
            @NotNull Long productoId,
            @NotNull @Positive Integer cantidad
    ) {}
}