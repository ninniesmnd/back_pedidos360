package com.pedidos.app.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQConfig {

    private final RabbitMQProperties props;

    public RabbitMQConfig(RabbitMQProperties props) {
        this.props = props;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // ---- Exchanges ----
    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(props.exchanges().pedidos(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.exchanges().dlx(), true, false);
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(props.queues().notificaciones())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.dlq().notificaciones())
                .build();
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.dlq().notificaciones()).build();
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue())
                .to(pedidosExchange())
                .with(props.routingKeys().bindingNotificaciones());
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq())
                .to(deadLetterExchange())
                .with(props.dlq().notificaciones());
    }

    @Bean
    public Queue ticketCocinaQueue() {
        return QueueBuilder.durable(props.queues().ticketCocina())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.dlq().ticketCocina())
                .build();
    }

    @Bean
    public Queue ticketCocinaDlq() {
        return QueueBuilder.durable(props.dlq().ticketCocina()).build();
    }

    @Bean
    public Binding ticketCocinaBinding() {
        return BindingBuilder.bind(ticketCocinaQueue())
                .to(pedidosExchange())
                .with(props.routingKeys().bindingTicketCocina());
    }

    @Bean
    public Binding ticketCocinaDlqBinding() {
        return BindingBuilder.bind(ticketCocinaDlq())
                .to(deadLetterExchange())
                .with(props.dlq().ticketCocina());
    }
}