package com.kafkastreaming.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record StreamingProperties(
        String bootstrapServers,
        String applicationId,
        String entrada,
        String salida,
        int replicas,
        int threads,
        long ventanaSegundos,
        long commitIntervalMs
) {}
