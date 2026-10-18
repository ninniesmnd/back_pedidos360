package com.rabbitadmin.app.messaging.producer;

import com.rabbitadmin.app.config.RabbitMQProperties;
import com.rabbitadmin.app.messaging.event.PedidoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    public void publicarPedidoCreado(PedidoEvent evento) {
        publicar(props.routingKeys().pedidoCreado(), evento);
    }

    public void publicarEstadoCambiado(PedidoEvent evento) {
        publicar(props.routingKeys().pedidoEstadoCambiado(), evento);
    }

    // Sin try/catch: si el broker falla, GlobalExceptionHandler responde 502 y el cliente lo registra.
    private void publicar(String routingKey, PedidoEvent evento) {
        rabbitTemplate.convertAndSend(props.exchanges().pedidos(), routingKey, evento);
        log.info("Evento {} publicado (pedidoId={}, routingKey={})",
                evento.tipoEvento(), evento.pedidoId(), routingKey);
    }
}