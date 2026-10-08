package com.ventas.app.config;

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

    @Bean
    public TopicExchange ventasExchange() {
        return new TopicExchange(props.exchanges().ventas(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.exchanges().dlx(), true, false);
    }

    // ---- Use case 1: low-stock alert ----
    @Bean
    public Queue alertasStockQueue() {
        return QueueBuilder.durable(props.queues().alertasStock())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.dlq().alertasStock())
                .build();
    }

    @Bean
    public Queue alertasStockDlq() {
        return QueueBuilder.durable(props.dlq().alertasStock()).build();
    }

    @Bean
    public Binding alertasStockBinding() {
        return BindingBuilder.bind(alertasStockQueue())
                .to(ventasExchange())
                .with(props.routingKeys().stockBajo());
    }

    @Bean
    public Binding alertasStockDlqBinding() {
        return BindingBuilder.bind(alertasStockDlq())
                .to(deadLetterExchange())
                .with(props.dlq().alertasStock());
    }

    // ---- Use case 2: sales receipt ----
    @Bean
    public Queue comprobantesQueue() {
        return QueueBuilder.durable(props.queues().comprobantes())
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(props.dlq().comprobantes())
                .build();
    }

    @Bean
    public Queue comprobantesDlq() {
        return QueueBuilder.durable(props.dlq().comprobantes()).build();
    }

    @Bean
    public Binding comprobantesBinding() {
        return BindingBuilder.bind(comprobantesQueue())
                .to(ventasExchange())
                .with(props.routingKeys().ventaRegistrada());
    }

    @Bean
    public Binding comprobantesDlqBinding() {
        return BindingBuilder.bind(comprobantesDlq())
                .to(deadLetterExchange())
                .with(props.dlq().comprobantes());
    }
}