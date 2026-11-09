package com.kafkaadmin.app.messaging.auditoria;

import com.kafkaadmin.app.config.KafkaTopicsProperties;
import com.kafkaadmin.app.messaging.KafkaEventParser;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditoriaConsumer {

    private final AuditoriaService auditoriaService;
    private final KafkaEventParser parser;
    private final KafkaTopicsProperties props;

    @KafkaListener(id = "auditoria-listener",
            topics = {"${app.kafka.entrada.name}", "${app.kafka.salida.name}"},
            groupId = "${app.kafka.groups.auditoria}",
            concurrency = "3")
    public void consumir(ConsumerRecord<String, String> record) {
        String tipo = record.topic().equals(props.entrada().name())
                ? parser.validarEventoNegocio(record)
                : parser.resultado(record.value()).tipo();
        auditoriaService.registrar(record, tipo);
    }
}
