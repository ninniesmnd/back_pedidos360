package com.pedidos.app.dto;

import com.pedidos.app.model.TipoDespacho;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CrearPedidoRequest(
        @NotNull Long localId,
        @NotNull TipoDespacho tipoDespacho,
        @NotEmpty @Valid List<ItemRequest> items
) {
    public record ItemRequest(
            @NotNull Long productoId,
            @NotNull String nombreProducto,
            @NotNull Integer cantidad,
            @NotNull Double precioUnitario
    ) {}
}