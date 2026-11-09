package com.ventas.app.messaging.producer;

import com.ventas.app.messaging.event.StockBajoEvent;
import com.ventas.app.messaging.event.VentaRegistradaEvent;
import com.ventas.app.model.Producto;
import com.ventas.app.model.Venta;
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

/** Stream de eventos de ventas e inventario en Kafka (key = localId). */
@Slf4j
@Component
public class VentasKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;
    private final int umbralStockBajo;

    public VentasKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${app.kafka.topic}") String topic,
                               @Value("${app.inventario.umbral-stock-bajo}") int umbralStockBajo) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.umbralStockBajo = umbralStockBajo;
    }

    public void publicarVentaRegistrada(Venta venta) {
        List<VentaRegistradaEvent.Item> items = venta.getItems().stream()
                .map(i -> new VentaRegistradaEvent.Item(i.getProductoId(), i.getNombreProducto(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        VentaRegistradaEvent evento = new VentaRegistradaEvent(venta.getId(), venta.getLocalId(),
                venta.getPedidoId(), venta.getTotal(), items, LocalDateTime.now());
        publicarTrasCommit("VENTA_REGISTRADA", String.valueOf(venta.getLocalId()), evento);
    }

    public void publicarStockBajoSiCorresponde(Producto producto) {
        if (producto.getStock() > umbralStockBajo) return;
        StockBajoEvent evento = new StockBajoEvent(producto.getId(), producto.getLocalId(),
                producto.getNombre(), producto.getStock(), umbralStockBajo, LocalDateTime.now());
        publicarTrasCommit("STOCK_BAJO", String.valueOf(producto.getLocalId()), evento);
    }

    private void publicarTrasCommit(String tipo, String key, Object evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(tipo, key, evento);
                }
            });
        } else {
            enviar(tipo, key, evento);
        }
    }

    private void enviar(String tipo, String key, Object evento) {
        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, evento);
            record.headers().add("tipoEvento", tipo.getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(record).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("Kafka: no se pudo publicar {} (key={}): {}", tipo, key, ex.getMessage());
                } else {
                    log.info("Kafka: {} key={} -> particion {} offset {}", tipo, key,
                            res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Kafka: error enviando {} (key={}): {}", tipo, key, ex.getMessage());
        }
    }
}
