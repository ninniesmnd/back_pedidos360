package com.pedidos.app.messaging.cocina;

import com.pedidos.app.messaging.error.MensajeNoProcesableException;
import com.pedidos.app.messaging.event.PedidoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TicketCocinaService {

    public String generarTicket(PedidoEvent evento) {
        if (evento == null || evento.pedidoId() == null
                || evento.items() == null || evento.items().isEmpty()) {
            throw new MensajeNoProcesableException("Evento de pedido incompleto: no se puede generar ticket de cocina");
        }

        StringBuilder ticket = new StringBuilder();
        ticket.append("=== TICKET COCINA ===\n")
                .append("Pedido #").append(evento.pedidoId())
                .append(" | Local ").append(evento.localId())
                .append(" | ").append(evento.tipoDespacho()).append('\n');
        evento.items().forEach(i -> ticket.append(" - ")
                .append(i.cantidad()).append(" x ").append(i.nombreProducto()).append('\n'));
        ticket.append("=====================");

        log.info("[TICKET-COCINA]\n{}", ticket);
        return ticket.toString();
    }
}