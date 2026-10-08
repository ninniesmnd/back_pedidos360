package com.pedidos.app.messaging.notification;

import com.pedidos.app.messaging.error.MensajeNoProcesableException;
import com.pedidos.app.messaging.event.PedidoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificacionService {

    public String notificar(PedidoEvent evento) {
        if (evento == null || evento.pedidoId() == null || evento.tipoEvento() == null) {
            throw new MensajeNoProcesableException("Evento de pedido incompleto: pedidoId y tipoEvento son obligatorios");
        }

        String mensaje = switch (evento.tipoEvento()) {
            case "PEDIDO_CREADO" ->
                    String.format("Tu pedido #%d fue recibido", evento.pedidoId());
            case "PEDIDO_ESTADO_CAMBIADO" ->
                    String.format("Tu pedido #%d cambio de %s a %s",
                            evento.pedidoId(), evento.estadoAnterior(), evento.estado());
            default -> throw new MensajeNoProcesableException(
                    "Tipo de evento desconocido: " + evento.tipoEvento());
        };

        log.info("[NOTIFICACION] para {}: {}", evento.clienteEmail(), mensaje);
        return mensaje;
    }
}