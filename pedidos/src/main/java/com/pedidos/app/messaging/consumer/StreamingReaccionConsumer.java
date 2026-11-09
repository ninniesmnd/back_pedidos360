package com.pedidos.app.messaging.consumer;

import com.pedidos.app.messaging.event.ResultadoStreamingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Consume el topico de salida de Kafka Streaming (analitica.salida) en el backend principal. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingReaccionConsumer {

    private final JsonMapper jsonMapper;
    private final StreamingReaccionService reaccionService;

    @KafkaListener(
            id = "pedidos-reaccion-listener",
            topics = "${app.kafka.salida}",
            groupId = "${app.kafka.group-reaccion}",
            concurrency = "3"
    )
    public void consumir(ConsumerRecord<String, String> record) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            log.warn("Mensaje vacio ignorado en {} particion={} offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }
        try {
            ResultadoStreamingEvent ev = jsonMapper.readValue(payload, ResultadoStreamingEvent.class);
            if (ev == null || ev.tipo() == null || ev.localId() == null || ev.valor() == null) {
                log.warn("ResultadoStreamingEvent incompleto ignorado en offset={}: {}", record.offset(), payload);
                return;
            }
            reaccionService.procesar(ev);
        } catch (JacksonException ex) {
            log.warn("JSON invalido ignorado en {} offset={}: {}", record.topic(), record.offset(), ex.getMessage());
        }
    }
}
