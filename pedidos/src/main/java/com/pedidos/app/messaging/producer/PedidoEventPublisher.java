package com.pedidos.app.messaging.producer;

import com.pedidos.app.config.RabbitMQProperties;
import com.pedidos.app.messaging.event.PedidoEvent;
import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    public void publicarPedidoCreado(Pedido pedido) {
        publicar(props.routingKeys().pedidoCreado(), construirEvento("PEDIDO_CREADO", pedido, null));
    }

    public void publicarEstadoCambiado(Pedido pedido, EstadoPedido estadoAnterior) {
        publicar(props.routingKeys().pedidoEstadoCambiado(),
                construirEvento("PEDIDO_ESTADO_CAMBIADO", pedido, estadoAnterior));
    }

    // If the broker is down the pedido is already saved: log and keep going (decoupled).
    private void publicar(String routingKey, PedidoEvent evento) {
        try {
            rabbitTemplate.convertAndSend(props.exchanges().pedidos(), routingKey, evento);
            log.info("Evento {} publicado (pedidoId={}, routingKey={})",
                    evento.tipoEvento(), evento.pedidoId(), routingKey);
        } catch (AmqpException ex) {
            log.error("No se pudo publicar el evento {} del pedido {}: {}",
                    evento.tipoEvento(), evento.pedidoId(), ex.getMessage());
        }
    }

    private PedidoEvent construirEvento(String tipo, Pedido pedido, EstadoPedido estadoAnterior) {
        List<PedidoEvent.Item> items = pedido.getItems().stream()
                .map(i -> new PedidoEvent.Item(i.getProductoId(), i.getNombreProducto(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        return new PedidoEvent(tipo, pedido.getId(), pedido.getLocalId(), pedido.getClienteEmail(),
                pedido.getEstado(), estadoAnterior, pedido.getTipoDespacho(), items, LocalDateTime.now());
    }
}