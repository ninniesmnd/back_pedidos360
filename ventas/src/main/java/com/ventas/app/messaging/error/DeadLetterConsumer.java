package com.ventas.app.messaging.error;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.dlq-logger.enabled", havingValue = "true")
public class DeadLetterConsumer {

    @RabbitListener(queues = {
            "${app.rabbitmq.dlq.alertas-stock}",
            "${app.rabbitmq.dlq.comprobantes}"
    })
    public void registrar(Message message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("[DLQ-REGISTRO] cola={} x-death={} payload={}",
                message.getMessageProperties().getConsumerQueue(),
                message.getMessageProperties().getHeaders().get("x-death"),
                new String(message.getBody(), StandardCharsets.UTF_8));
        channel.basicAck(deliveryTag, false);
    }
}