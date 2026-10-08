package com.ventas.app.messaging.comprobante;

import com.ventas.app.messaging.error.MensajeNoProcesableException;
import com.ventas.app.messaging.event.VentaRegistradaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ComprobanteService {

    public String generarComprobante(VentaRegistradaEvent evento) {
        if (evento == null || evento.ventaId() == null || evento.total() == null
                || evento.items() == null || evento.items().isEmpty()) {
            throw new MensajeNoProcesableException("Evento de venta incompleto: no se puede generar comprobante");
        }

        StringBuilder doc = new StringBuilder();
        doc.append("=== COMPROBANTE DE VENTA ===\n")
                .append("Venta #").append(evento.ventaId())
                .append(" | Local ").append(evento.localId());
        if (evento.pedidoId() != null) {
            doc.append(" | Pedido #").append(evento.pedidoId());
        }
        doc.append('\n');
        evento.items().forEach(i -> doc.append(" - ").append(i.cantidad()).append(" x ")
                .append(i.nombreProducto()).append(" @ ").append(i.precioUnitario()).append('\n'));
        doc.append("TOTAL: ").append(evento.total()).append('\n')
                .append("============================");

        log.info("[COMPROBANTE]\n{}", doc);
        return doc.toString();
    }
}