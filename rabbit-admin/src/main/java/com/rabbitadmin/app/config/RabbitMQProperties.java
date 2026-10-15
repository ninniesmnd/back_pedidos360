package com.rabbitadmin.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMQProperties(
        Exchanges exchanges, Queues queues, Dlq dlq, RoutingKeys routingKeys, Retry retry
) {
    public record Exchanges(String pedidos, String pedidosDlx, String ventas, String ventasDlx) {}
    public record Queues(String notificaciones, String ticketCocina, String alertasStock, String comprobantes) {}
    public record Dlq(String notificaciones, String ticketCocina, String alertasStock, String comprobantes) {}
    public record RoutingKeys(String bindingNotificaciones, String bindingTicketCocina,
                              String stockBajo, String ventaRegistrada) {}
    public record Retry(int maxIntentos, long backoffMs) {}
}