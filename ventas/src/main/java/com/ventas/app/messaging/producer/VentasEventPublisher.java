package com.ventas.app.messaging.producer;

import com.ventas.app.messaging.event.StockBajoEvent;
import com.ventas.app.messaging.event.VentaRegistradaEvent;
import com.ventas.app.model.Producto;
import com.ventas.app.model.Venta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Cliente de rabbit-admin: la publicacion real a RabbitMQ ocurre alla. */
@Slf4j
@Component
public class VentasEventPublisher {

    private final RestClient restClient;
    private final int umbralStockBajo;

    public VentasEventPublisher(@Value("${app.rabbit-admin.url:http://localhost:8082}") String rabbitAdminUrl,
                                @Value("${app.internal.api-key:dev-internal-key}") String apiKey,
                                @Value("${app.inventario.umbral-stock-bajo}") int umbralStockBajo) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(rabbitAdminUrl)
                .defaultHeader("X-Internal-Key", apiKey)
                .requestFactory(factory)
                .build();
        this.umbralStockBajo = umbralStockBajo;
    }

    public void publicarStockBajoSiCorresponde(Producto producto) {
        if (producto.getStock() > umbralStockBajo) {
            return;
        }
        StockBajoEvent evento = new StockBajoEvent(producto.getId(), producto.getLocalId(),
                producto.getNombre(), producto.getStock(), umbralStockBajo, LocalDateTime.now());
        publicarTrasCommit("/api/eventos/ventas/stock-bajo", evento);
    }

    public void publicarVentaRegistrada(Venta venta) {
        List<VentaRegistradaEvent.Item> items = venta.getItems().stream()
                .map(i -> new VentaRegistradaEvent.Item(i.getProductoId(), i.getNombreProducto(),
                        i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        VentaRegistradaEvent evento = new VentaRegistradaEvent(venta.getId(), venta.getLocalId(),
                venta.getPedidoId(), venta.getTotal(), items, LocalDateTime.now());
        publicarTrasCommit("/api/eventos/ventas/registrada", evento);
    }

    private void publicarTrasCommit(String uri, Object evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(uri, evento);
                }
            });
        } else {
            enviar(uri, evento);
        }
    }

    // Si rabbit-admin o el broker caen, la venta ya esta guardada: se registra y se sigue (desacoplado).
    private void enviar(String uri, Object evento) {
        try {
            restClient.post()
                    .uri(uri)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento {} enviado a rabbit-admin (uri={})", evento.getClass().getSimpleName(), uri);
        } catch (Exception ex) {
            log.error("No se pudo enviar el evento {} a rabbit-admin (uri={}): {}",
                    evento.getClass().getSimpleName(), uri, ex.getMessage());
        }
    }
}