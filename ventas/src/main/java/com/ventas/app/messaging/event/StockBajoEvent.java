package com.ventas.app.messaging.event;

import java.time.LocalDateTime;

public record StockBajoEvent(Long productoId, Long localId, String nombreProducto,
                             Integer stockActual, Integer umbral, LocalDateTime ocurridoEn) {}