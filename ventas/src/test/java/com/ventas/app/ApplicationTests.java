package com.ventas.app;

import com.ventas.app.messaging.event.StockBajoEvent;
import com.ventas.app.messaging.event.VentaRegistradaEvent;
import com.ventas.app.messaging.producer.VentasKafkaProducer;
import com.ventas.app.model.ItemVenta;
import com.ventas.app.model.Producto;
import com.ventas.app.model.Venta;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplicationTests {

    @Test
    @SuppressWarnings("unchecked")
    void ventasKafkaProducerPublicaVentaYStockBajoCuandoCorresponde() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("negocio.eventos", 0), 0L, 5, 0L, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

        VentasKafkaProducer producer = new VentasKafkaProducer(template, "negocio.eventos", 5);

        Venta venta = new Venta();
        venta.setId(99L);
        venta.setLocalId(1L);
        venta.setPedidoId(12L);
        venta.setTotal(17000.0);
        ItemVenta item = new ItemVenta();
        item.setProductoId(101L);
        item.setNombreProducto("Hamburguesa");
        item.setCantidad(2);
        item.setPrecioUnitario(8500.0);
        venta.agregarItem(item);

        producer.publicarVentaRegistrada(venta);

        // Producto con stock > umbral (10 > 5) NO debe publicar
        Producto suficiente = new Producto();
        suficiente.setId(101L);
        suficiente.setLocalId(1L);
        suficiente.setNombre("Hamburguesa");
        suficiente.setStock(10);
        producer.publicarStockBajoSiCorresponde(suficiente);

        // Producto con stock <= umbral (4 <= 5) SI debe publicar
        Producto bajo = new Producto();
        bajo.setId(201L);
        bajo.setLocalId(1L);
        bajo.setNombre("Bebida Zero");
        bajo.setStock(4);
        producer.publicarStockBajoSiCorresponde(bajo);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template, times(2)).send(captor.capture());

        ProducerRecord<String, Object> recVenta = captor.getAllValues().get(0);
        assertThat(recVenta.topic()).isEqualTo("negocio.eventos");
        assertThat(recVenta.key()).isEqualTo("1");
        assertThat(new String(recVenta.headers().lastHeader("tipoEvento").value(), StandardCharsets.UTF_8))
                .isEqualTo("VENTA_REGISTRADA");
        assertThat(recVenta.value()).isInstanceOf(VentaRegistradaEvent.class);

        ProducerRecord<String, Object> recStock = captor.getAllValues().get(1);
        assertThat(new String(recStock.headers().lastHeader("tipoEvento").value(), StandardCharsets.UTF_8))
                .isEqualTo("STOCK_BAJO");
        assertThat(recStock.value()).isInstanceOf(StockBajoEvent.class);
    }
}
