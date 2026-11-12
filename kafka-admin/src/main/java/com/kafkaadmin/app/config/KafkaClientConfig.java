package com.kafkaadmin.app.config;

import com.kafkaadmin.app.messaging.error.MensajeInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaClientConfig {

    /** Cliente de administracion que usa KafkaAdminService. */
    @Bean(destroyMethod = "close")
    public Admin adminClient(KafkaAdmin kafkaAdmin) {
        return Admin.create(kafkaAdmin.getConfigurationProperties());
    }

    /**
     * Boot lo detecta y lo aplica a los @KafkaListener.
     * Reintenta (backoff fijo) y luego manda el mensaje a <topic>.DLT (particion 0) dejando log.
     * Mensajes invalidos (MensajeInvalidoException) no se reintentan: van directo a la DLT.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> template,
                                                 KafkaTopicsProperties props) {
        DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(KafkaTopicsProperties.dltDe(record.topic()), 0));

        ConsumerRecordRecoverer recoverer = (record, ex) -> {
            log.error("[DLT] topic={} partition={} offset={} key={} causa={}",
                    record.topic(), record.partition(), record.offset(), record.key(), ex.getMessage());
            dlt.accept(record, ex);
        };

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(props.retry().backoffMs(), props.retry().maxReintentos()));
        handler.addNotRetryableExceptions(MensajeInvalidoException.class);
        handler.setRetryListeners((record, ex, intento) ->
                log.warn("Reintento {} fallido topic={} offset={}: {}",
                        intento, record.topic(), record.offset(), ex.getMessage()));
        return handler;
    }
}
