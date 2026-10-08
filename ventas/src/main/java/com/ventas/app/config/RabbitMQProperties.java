package com.ventas.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMQProperties(
        Exchanges exchanges,
        Queues queues,
        Dlq dlq,
        RoutingKeys routingKeys,
        Retry retry
) {
    public record Exchanges(String ventas, String dlx) {}
    public record Queues(String alertasStock, String comprobantes) {}
    public record Dlq(String alertasStock, String comprobantes) {}
    public record RoutingKeys(String stockBajo, String ventaRegistrada) {}
    public record Retry(int maxIntentos, long backoffMs) {}
}