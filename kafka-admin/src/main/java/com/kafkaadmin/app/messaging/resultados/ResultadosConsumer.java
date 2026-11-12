package com.kafkaadmin.app.messaging.resultados;

import com.kafkaadmin.app.messaging.KafkaEventParser;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResultadosConsumer {

    private final KafkaEventParser parser;
    private final ResultadosService service;

    @KafkaListener(id = "resultados-listener",
            topics = "${app.kafka.salida.name}",
            groupId = "${app.kafka.groups.analitica}",
            concurrency = "3")
    public void consumir(ConsumerRecord<String, String> record) {
        service.registrar(parser.resultado(record.value()));
    }
}
