package com.pedidos.app.messaging.error;

import com.pedidos.app.config.RabbitMQProperties;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * What to do when a consumer fails:
 *  - Non-recoverable error (invalid message)   -> NACK without requeue -> DLQ
 *  - Recoverable error, attempts left          -> re-queue with x-retry-count + 1, ACK the original
 *  - Recoverable error, attempts exhausted     -> NACK without requeue -> DLQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MensajeriaErrorHandler {

    public static final String HEADER_REINTENTOS = "x-retry-count";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    public void manejar(Exception ex, Message message, Channel channel, long deliveryTag) throws IOException {
        String cola = message.getMessageProperties().getConsumerQueue();
        int intentos = leerIntentos(message);

        if (esNoRecuperable(ex)) {
            log.error("[DLQ] Mensaje no procesable en cola={} (sin reintento). Causa: {}", cola, ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        if (intentos >= props.retry().maxIntentos()) {
            log.error("[DLQ] Reintentos agotados ({}/{}) en cola={}. Ultima causa: {}",
                    intentos, props.retry().maxIntentos(), cola, ex.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        log.warn("Fallo recuperable en cola={} (intento {}/{}): {}. Reintentando...",
                cola, intentos + 1, props.retry().maxIntentos(), ex.getMessage());
        pausar(props.retry().backoffMs());

        try {
            message.getMessageProperties().setHeader(HEADER_REINTENTOS, intentos + 1);
            // Default exchange ("") + queue name = straight to THAT queue (no duplicates in the other queues)
            rabbitTemplate.send("", cola, message);
            channel.basicAck(deliveryTag, false);
        } catch (AmqpException reenvioEx) {
            log.error("[DLQ] No se pudo reencolar el mensaje en cola={}: {}", cola, reenvioEx.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private boolean esNoRecuperable(Exception ex) {
        return ex instanceof MensajeNoProcesableException
                || ex instanceof IllegalArgumentException
                || ex instanceof MessageConversionException;
    }

    private int leerIntentos(Message message) {
        Object valor = message.getMessageProperties().getHeaders().get(HEADER_REINTENTOS);
        return valor instanceof Number n ? n.intValue() : 0;
    }

    private void pausar(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}