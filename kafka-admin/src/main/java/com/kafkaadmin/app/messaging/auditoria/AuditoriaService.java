package com.kafkaadmin.app.messaging.auditoria;

import com.kafkaadmin.app.messaging.error.MensajeInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

/** Registro de trazabilidad: una linea por evento/resultado, en el logger AUDITORIA. */
@Slf4j(topic = "AUDITORIA")
@Service
public class AuditoriaService {

    public void registrar(ConsumerRecord<String, String> record, String tipo) {
        if (record.value() == null || record.value().isBlank()) {
            throw new MensajeInvalidoException("Evento de auditoria sin payload");
        }
        log.info("[AUDITORIA] topic={} particion={} offset={} key={} tipo={} payload={}",
                record.topic(), record.partition(), record.offset(), record.key(), tipo, record.value());
    }
}
