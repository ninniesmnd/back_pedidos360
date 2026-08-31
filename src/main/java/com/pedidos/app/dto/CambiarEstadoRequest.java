package com.pedidos.app.dto;

import com.pedidos.app.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull EstadoPedido nuevoEstado
) {}