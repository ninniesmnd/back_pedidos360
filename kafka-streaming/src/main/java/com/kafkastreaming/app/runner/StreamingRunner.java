package com.kafkastreaming.app.runner;

import com.kafkastreaming.app.config.StreamingProperties;
import com.kafkastreaming.app.topology.AnaliticaTopology;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingRunner {

    private final StreamingProperties props;
    private final AnaliticaTopology topology;
    private volatile KafkaStreams streams;

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        new Thread(this::arrancar, "streams-bootstrap").start();
    }

    public String estado() {
        KafkaStreams ks = streams;
        return ks == null ? "STARTING_WAITING_FOR_TOPICS" : ks.state().name();
    }

    private void arrancar() {
        try {
            esperarTopicos();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        Properties p = new Properties();
        p.put(StreamsConfig.APPLICATION_ID_CONFIG, props.applicationId());
        p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        p.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, props.threads());
        p.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, props.replicas());
        p.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, props.commitIntervalMs());
        p.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);
        p.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        p.put(StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest");
        p.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "all");

        KafkaStreams ks = new KafkaStreams(topology.construir(), p);
        ks.setStateListener((nuevo, anterior) -> log.info("Kafka Streams: {} -> {}", anterior, nuevo));
        ks.setUncaughtExceptionHandler(ex -> {
            log.error("Error en Kafka Streams, reemplazando hilo", ex);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
        });
        ks.start();
        streams = ks;
    }

    /** kafka-admin crea los topicos al arrancar; aqui esperamos a que existan. */
    private void esperarTopicos() throws InterruptedException {
        Properties ap = new Properties();
        ap.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        while (!Thread.currentThread().isInterrupted()) {
            // Admin.create dentro del try: si Kafka aun no resuelve por DNS, se reintenta
            try (Admin admin = Admin.create(ap)) {
                Set<String> existentes = admin.listTopics().names().get(10, TimeUnit.SECONDS);
                if (existentes.contains(props.entrada()) && existentes.contains(props.salida())) {
                    log.info("Topicos {} y {} disponibles", props.entrada(), props.salida());
                    return;
                }
                log.info("Esperando topicos {} y {} (los crea kafka-admin)...", props.entrada(), props.salida());
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("Kafka aun no disponible: {}", ex.getMessage());
            }
            Thread.sleep(5000);
        }
    }

    
    @PreDestroy
    public void detener() {
        if (streams != null) streams.close(Duration.ofSeconds(10));
    }
}
