package com.pedidos.app.messaging.producer;

import com.pedidos.app.messaging.event.PedidoEvent;
import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.Pedido;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/** Publica el stream de eventos de pedidos en Kafka (key = localId para agrupar por local en Kafka Streams). */
@Slf4j
@Component
public class PedidoKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public PedidoKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        publicarTrasCommit(construir("PEDIDO_CREADO", pedido, null));
    }

    public void publicarEstadoCambiado(Pedido pedido, EstadoPedido estadoAnterior) {
        publicarTrasCommit(construir("PEDIDO_ESTADO_CAMBIADO", pedido, estadoAnterior));
    }

    private void publicarTrasCommit(PedidoEvent evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(evento);
                }
            });
        } else {
            enviar(evento);
        }
    }

    private void enviar(PedidoEvent evento) {
        try {
            ProducerRecord<String, Object> record =
                    new ProducerRecord<>(topic, String.valueOf(evento.localId()), evento);
            record.headers().add("tipoEvento", evento.tipoEvento().getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(record).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("Kafka: no se pudo publicar {} del pedido {}: {}",
                            evento.tipoEvento(), evento.pedidoId(), ex.getMessage());
                } else {
                    log.info("Kafka: {} pedidoId={} localId={} -> particion {} offset {}", evento.tipoEvento(),
                            evento.pedidoId(), evento.localId(),
                            res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Kafka: error enviando {} del pedido {}: {}", evento.tipoEvento(), evento.pedidoId(), ex.getMessage());
        }
    }

    private PedidoEvent construir(String tipo, Pedido pedido, EstadoPedido anterior) {
        List<PedidoEvent.Item> items = pedido.getItems().stream()
                .map(i -> new PedidoEvent.Item(i.getProductoId(), i.getNombreProducto(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        return new PedidoEvent(tipo, pedido.getId(), pedido.getLocalId(), pedido.getClienteEmail(),
                pedido.getEstado(), anterior, pedido.getTipoDespacho(), items, LocalDateTime.now());
    }
}
