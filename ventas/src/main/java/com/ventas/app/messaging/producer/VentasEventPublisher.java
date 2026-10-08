package com.ventas.app.messaging.producer;

import com.ventas.app.config.RabbitMQProperties;
import com.ventas.app.messaging.event.StockBajoEvent;
import com.ventas.app.messaging.event.VentaRegistradaEvent;
import com.ventas.app.model.Producto;
import com.ventas.app.model.Venta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class VentasEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;
    private final int umbralStockBajo;

    public VentasEventPublisher(RabbitTemplate rabbitTemplate,
                                RabbitMQProperties props,
                                @Value("${app.inventario.umbral-stock-bajo}") int umbralStockBajo) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
        this.umbralStockBajo = umbralStockBajo;
    }

    public void publicarStockBajoSiCorresponde(Producto producto) {
        if (producto.getStock() > umbralStockBajo) {
            return;
        }
        StockBajoEvent evento = new StockBajoEvent(producto.getId(), producto.getLocalId(),
                producto.getNombre(), producto.getStock(), umbralStockBajo, LocalDateTime.now());
        publicarTrasCommit(props.routingKeys().stockBajo(), evento);
    }

    public void publicarVentaRegistrada(Venta venta) {
        List<VentaRegistradaEvent.Item> items = venta.getItems().stream()
                .map(i -> new VentaRegistradaEvent.Item(i.getProductoId(), i.getNombreProducto(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        VentaRegistradaEvent evento = new VentaRegistradaEvent(venta.getId(), venta.getLocalId(),
                venta.getPedidoId(), venta.getTotal(), items, LocalDateTime.now());
        publicarTrasCommit(props.routingKeys().ventaRegistrada(), evento);
    }

    private void publicarTrasCommit(String routingKey, Object evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(routingKey, evento);
                }
            });
        } else {
            enviar(routingKey, evento);
        }
    }

    private void enviar(String routingKey, Object evento) {
        try {
            rabbitTemplate.convertAndSend(props.exchanges().ventas(), routingKey, evento);
            log.info("Evento publicado (routingKey={}, tipo={})", routingKey, evento.getClass().getSimpleName());
        } catch (AmqpException ex) {
            log.error("No se pudo publicar el evento {} (routingKey={}): {}",
                    evento.getClass().getSimpleName(), routingKey, ex.getMessage());
        }
    }
}