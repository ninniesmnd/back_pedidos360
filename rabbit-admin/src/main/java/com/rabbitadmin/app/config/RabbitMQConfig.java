package com.rabbitadmin.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/** Plano de control: toda la topología del sistema vive aquí. */
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RabbitMQProperties props;

    private record Cola(String nombre, String dlq, String bindingKey) {}

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Declarables pedidosTopology() {
        var e = props.exchanges(); var q = props.queues(); var d = props.dlq(); var k = props.routingKeys();
        return dominio(new TopicExchange(e.pedidos(), true, false),
                new DirectExchange(e.pedidosDlx(), true, false),
                new Cola(q.notificaciones(), d.notificaciones(), k.bindingNotificaciones()),
                new Cola(q.ticketCocina(), d.ticketCocina(), k.bindingTicketCocina()));
    }

    @Bean
    public Declarables ventasTopology() {
        var e = props.exchanges(); var q = props.queues(); var d = props.dlq(); var k = props.routingKeys();
        return dominio(new TopicExchange(e.ventas(), true, false),
                new DirectExchange(e.ventasDlx(), true, false),
                new Cola(q.alertasStock(), d.alertasStock(), k.stockBajo()),
                new Cola(q.comprobantes(), d.comprobantes(), k.ventaRegistrada()));
    }

    /** Exchange + DLX + por cada cola: principal (con DLX), su DLQ y ambos bindings. */
    private Declarables dominio(TopicExchange exchange, DirectExchange dlx, Cola... colas) {
        List<Declarable> out = new ArrayList<>(List.of(exchange, dlx));
        for (Cola c : colas) {
            Queue principal = QueueBuilder.durable(c.nombre())
                    .deadLetterExchange(dlx.getName())
                    .deadLetterRoutingKey(c.dlq())
                    .build();
            Queue muerta = QueueBuilder.durable(c.dlq()).build();
            out.add(principal);
            out.add(muerta);
            out.add(BindingBuilder.bind(principal).to(exchange).with(c.bindingKey()));
            out.add(BindingBuilder.bind(muerta).to(dlx).with(c.dlq()));
        }
        return new Declarables(out);
    }
}