package com.ventas.app.messaging.event;

import java.time.LocalDateTime;
import java.util.List;

public record VentaRegistradaEvent(Long ventaId, Long localId, Long pedidoId, Double total,
                                   List<Item> items, LocalDateTime ocurridoEn) {
    public record Item(Long productoId, String nombreProducto, Integer cantidad, Double precioUnitario) {}
}