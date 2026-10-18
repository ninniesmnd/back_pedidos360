package com.rabbitadmin.app.messaging.producer;

import com.rabbitadmin.app.config.RabbitMQProperties;
import com.rabbitadmin.app.messaging.event.StockBajoEvent;
import com.rabbitadmin.app.messaging.event.VentaRegistradaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VentasEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    public void publicarStockBajo(StockBajoEvent evento) {
        enviar(props.routingKeys().stockBajo(), evento);
    }

    public void publicarVentaRegistrada(VentaRegistradaEvent evento) {
        enviar(props.routingKeys().ventaRegistrada(), evento);
    }

    // Sin try/catch: si el broker falla, GlobalExceptionHandler responde 502 y el cliente lo registra.
    private void enviar(String routingKey, Object evento) {
        rabbitTemplate.convertAndSend(props.exchanges().ventas(), routingKey, evento);
        log.info("Evento publicado (routingKey={}, tipo={})", routingKey, evento.getClass().getSimpleName());
    }
}