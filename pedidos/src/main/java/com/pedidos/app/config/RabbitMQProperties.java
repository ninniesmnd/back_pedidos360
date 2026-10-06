package com.pedidos.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMQProperties(
        Exchanges exchanges,
        Queues queues,
        Dlq dlq,
        RoutingKeys routingKeys,
        Retry retry
) {
    public record Exchanges(String pedidos, String dlx) {}
    public record Queues(String notificaciones, String ticketCocina) {}
    public record Dlq(String notificaciones, String ticketCocina) {}
    public record RoutingKeys(String pedidoCreado, String pedidoEstadoCambiado,
                              String bindingNotificaciones, String bindingTicketCocina) {}
    public record Retry(int maxIntentos, long backoffMs) {}
}