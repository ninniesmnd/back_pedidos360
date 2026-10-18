package com.pedidos.app.messaging.producer;

import com.pedidos.app.messaging.event.PedidoEvent;
import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.Pedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Cliente de rabbit-admin: la publicacion real a RabbitMQ ocurre alla. */
@Slf4j
@Component
public class PedidoEventPublisher {

    private final RestClient restClient;

    public PedidoEventPublisher(@Value("${app.rabbit-admin.url:http://localhost:8082}") String rabbitAdminUrl,
                                @Value("${app.internal.api-key:dev-internal-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(rabbitAdminUrl)
                .defaultHeader("X-Internal-Key", apiKey)
                .requestFactory(factory)
                .build();
    }

    public void publicarPedidoCreado(Pedido pedido) {
        enviar("/api/eventos/pedidos/creado", construirEvento("PEDIDO_CREADO", pedido, null));
    }

    public void publicarEstadoCambiado(Pedido pedido, EstadoPedido estadoAnterior) {
        enviar("/api/eventos/pedidos/estado-cambiado",
                construirEvento("PEDIDO_ESTADO_CAMBIADO", pedido, estadoAnterior));
    }

    // Si rabbit-admin o el broker caen, el pedido ya esta guardado: se registra y se sigue (desacoplado).
    private void enviar(String uri, PedidoEvent evento) {
        try {
            restClient.post()
                    .uri(uri)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento {} enviado a rabbit-admin (pedidoId={})", evento.tipoEvento(), evento.pedidoId());
        } catch (Exception ex) {
            log.error("No se pudo enviar el evento {} del pedido {} a rabbit-admin: {}",
                    evento.tipoEvento(), evento.pedidoId(), ex.getMessage());
        }
    }

    private PedidoEvent construirEvento(String tipo, Pedido pedido, EstadoPedido estadoAnterior) {
        List<PedidoEvent.Item> items = pedido.getItems().stream()
                .map(i -> new PedidoEvent.Item(i.getProductoId(), i.getNombreProducto(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        return new PedidoEvent(tipo, pedido.getId(), pedido.getLocalId(), pedido.getClienteEmail(),
                pedido.getEstado(), estadoAnterior, pedido.getTipoDespacho(), items, LocalDateTime.now());
    }
}