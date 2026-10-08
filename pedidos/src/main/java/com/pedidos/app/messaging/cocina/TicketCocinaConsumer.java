package com.pedidos.app.messaging.cocina;

import com.pedidos.app.messaging.error.MensajeriaErrorHandler;
import com.pedidos.app.messaging.event.PedidoEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TicketCocinaConsumer {

    private final TicketCocinaService ticketCocinaService;
    private final MensajeriaErrorHandler errorHandler;

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-cocina}")
    public void consumir(@Payload PedidoEvent evento,
                         Message message,
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            ticketCocinaService.generarTicket(evento);
        } catch (Exception ex) {
            errorHandler.manejar(ex, message, channel, deliveryTag);
            return;
        }
        channel.basicAck(deliveryTag, false);
    }
}