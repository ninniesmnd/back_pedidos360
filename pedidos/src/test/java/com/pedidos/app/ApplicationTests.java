package com.pedidos.app;

import com.pedidos.app.messaging.consumer.StreamingReaccionConsumer;
import com.pedidos.app.messaging.consumer.StreamingReaccionService;
import com.pedidos.app.messaging.event.PedidoEvent;
import com.pedidos.app.messaging.producer.PedidoKafkaProducer;
import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.ItemPedido;
import com.pedidos.app.model.Pedido;
import com.pedidos.app.model.TipoDespacho;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplicationTests {

    @Test
    @SuppressWarnings("unchecked")
    void pedidoKafkaProducerPublicaConKeyLocalIdYHeaderTipoEvento() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("negocio.eventos", 1), 0L, 10, 0L, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

        PedidoKafkaProducer producer = new PedidoKafkaProducer(template, "negocio.eventos");

        Pedido pedido = new Pedido();
        pedido.setId(42L);
        pedido.setLocalId(2L);
        pedido.setClienteEmail("cliente@caso360.onmicrosoft.com");
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setTipoDespacho(TipoDespacho.DELIVERY);
        ItemPedido item = new ItemPedido();
        item.setProductoId(101L);
        item.setNombreProducto("Hamburguesa");
        item.setCantidad(2);
        item.setPrecioUnitario(8500.0);
        pedido.agregarItem(item);

        producer.publicarPedidoCreado(pedido);

        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template, times(1)).send(captor.capture());

        ProducerRecord<String, Object> enviado = captor.getValue();
        assertThat(enviado.topic()).isEqualTo("negocio.eventos");
        assertThat(enviado.key()).isEqualTo("2");
        assertThat(new String(enviado.headers().lastHeader("tipoEvento").value(), StandardCharsets.UTF_8))
                .isEqualTo("PEDIDO_CREADO");
        assertThat(enviado.value()).isInstanceOf(PedidoEvent.class);
        PedidoEvent payload = (PedidoEvent) enviado.value();
        assertThat(payload.pedidoId()).isEqualTo(42L);
        assertThat(payload.localId()).isEqualTo(2L);
    }

    @Test
    void streamingReaccionConsumerRegistraResultadosYFiltraPorLocal() {
        StreamingReaccionService service = new StreamingReaccionService();
        StreamingReaccionConsumer consumer = new StreamingReaccionConsumer(JsonMapper.shared(), service);

        // 1. Mensaje malformado no debe romper el consumidor
        consumer.consumir(new ConsumerRecord<>("analitica.salida", 0, 1L, "1", "no-es-json"));
        assertThat(service.listarPorLocal(null)).isEmpty();

        // 2. Mensaje valido de local 1 y alerta de local 2
        consumer.consumir(new ConsumerRecord<>("analitica.salida", 0, 2L, "1",
                "{\"tipo\":\"VENTAS_TOTAL\",\"localId\":1,\"clave\":\"total\",\"valor\":18000.0,\"ventanaInicio\":\"2026-11-09T10:00:00Z\",\"ventanaFin\":\"2026-11-09T10:01:00Z\"}"));
        consumer.consumir(new ConsumerRecord<>("analitica.salida", 1, 3L, "2",
                "{\"tipo\":\"STOCK_BAJO\",\"localId\":2,\"clave\":\"Pizza Pepperoni\",\"valor\":2.0}"));

        assertThat(service.listarPorLocal(null)).hasSize(2);
        assertThat(service.listarPorLocal(1L)).hasSize(1);
        assertThat(service.listarPorLocal(1L).get(0).tipo()).isEqualTo("VENTAS_TOTAL");
        assertThat(service.listarPorLocal(2L)).hasSize(1);
        assertThat(service.listarPorLocal(2L).get(0).clave()).isEqualTo("Pizza Pepperoni");
    }
}
