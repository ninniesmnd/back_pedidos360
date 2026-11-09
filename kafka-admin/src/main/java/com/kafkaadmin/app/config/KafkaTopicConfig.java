package com.kafkaadmin.app.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Toda la topologia Kafka vive aqui. KafkaAdmin crea/ajusta estos topicos al arrancar. */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaTopicsProperties props;

    @Bean
    public NewTopic pedidosTopic() {
        var t = props.pedidos();
        return topic(t.name(), t.partitions(), t.retentionMs(), t.cleanupPolicy());
    }

    @Bean
    public NewTopic ventasTopic() {
        var t = props.ventas();
        return topic(t.name(), t.partitions(), t.retentionMs(), t.cleanupPolicy());
    }

    @Bean
    public NewTopic pedidosDlt() {
        return topic(KafkaTopicsProperties.dltDe(props.pedidos().name()),
                props.dlt().partitions(), props.dlt().retentionMs(), TopicConfig.CLEANUP_POLICY_DELETE);
    }

    @Bean
    public NewTopic ventasDlt() {
        return topic(KafkaTopicsProperties.dltDe(props.ventas().name()),
                props.dlt().partitions(), props.dlt().retentionMs(), TopicConfig.CLEANUP_POLICY_DELETE);
    }

    private NewTopic topic(String name, int partitions, long retentionMs, String cleanupPolicy) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(props.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }
}