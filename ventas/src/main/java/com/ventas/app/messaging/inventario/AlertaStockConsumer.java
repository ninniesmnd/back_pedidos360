package com.ventas.app.messaging.inventario;

import com.rabbitmq.client.Channel;
import com.ventas.app.messaging.error.MensajeriaErrorHandler;
import com.ventas.app.messaging.event.StockBajoEvent;
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
public class AlertaStockConsumer {

    private final AlertaStockService alertaStockService;
    private final MensajeriaErrorHandler errorHandler;

    @RabbitListener(queues = "${app.rabbitmq.queues.alertas-stock}")
    public void consumir(@Payload StockBajoEvent evento,
                         Message message,
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            alertaStockService.registrarAlerta(evento);
        } catch (Exception ex) {
            errorHandler.manejar(ex, message, channel, deliveryTag);
            return;
        }
        channel.basicAck(deliveryTag, false);
    }
}