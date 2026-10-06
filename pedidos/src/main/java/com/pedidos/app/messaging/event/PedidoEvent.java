package com.pedidos.app.messaging.event;

import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.TipoDespacho;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoEvent(
        String tipoEvento,          // PEDIDO_CREADO | PEDIDO_ESTADO_CAMBIADO
        Long pedidoId,
        Long localId,
        String clienteEmail,
        EstadoPedido estado,
        EstadoPedido estadoAnterior,
        TipoDespacho tipoDespacho,
        List<Item> items,
        LocalDateTime ocurridoEn
) {
    public record Item(Long productoId, String nombreProducto, Integer cantidad, Double precioUnitario) {}
}