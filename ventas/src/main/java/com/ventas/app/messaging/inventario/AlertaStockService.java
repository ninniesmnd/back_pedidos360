package com.ventas.app.messaging.inventario;

import com.ventas.app.messaging.error.MensajeNoProcesableException;
import com.ventas.app.messaging.event.StockBajoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertaStockService {

    public void registrarAlerta(StockBajoEvent evento) {
        if (evento == null || evento.productoId() == null || evento.stockActual() == null) {
            throw new MensajeNoProcesableException("Evento de stock incompleto: productoId y stockActual son obligatorios");
        }
        log.warn("[ALERTA-STOCK] local {}: '{}' (id {}) quedo con {} unidades (umbral {})",
                evento.localId(), evento.nombreProducto(), evento.productoId(),
                evento.stockActual(), evento.umbral());
    }
}