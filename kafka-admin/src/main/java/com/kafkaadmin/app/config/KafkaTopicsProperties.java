package com.kafkaadmin.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaTopicsProperties(
        int replicas,
        int minInsyncReplicas,
        Topic entrada,
        Topic salida,
        Dlt dlt,
        Retry retry
) {
    public record Topic(String name, int partitions, long retentionMs, String cleanupPolicy) {}
    public record Dlt(int partitions, long retentionMs) {}
    public record Retry(long backoffMs, int maxReintentos) {}

    public static String dltDe(String topic) {
        return topic + ".DLT";
    }
}
