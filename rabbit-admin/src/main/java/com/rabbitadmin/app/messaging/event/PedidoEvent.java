package com.rabbitadmin.app.messaging.event;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoEvent(
        String tipoEvento,          // PEDIDO_CREADO | PEDIDO_ESTADO_CAMBIADO
        Long pedidoId,
        Long localId,
        String clienteEmail,
        String estado,
        String estadoAnterior,
        String tipoDespacho,
        List<Item> items,
        LocalDateTime ocurridoEn
) {
    public record Item(Long productoId, String nombreProducto, Integer cantidad, Double precioUnitario) {}
}