package com.kafkaadmin.app.service;

import com.kafkaadmin.app.config.KafkaTopicsProperties;
import com.kafkaadmin.app.dto.KafkaAdminDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Toda la logica de administracion de Kafka queda encapsulada aqui. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAdminService {

    private static final Set<String> CONFIGS_PERMITIDAS = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy",
            "min.insync.replicas", "segment.bytes", "max.message.bytes");

    private final Admin admin;
    private final KafkaTopicsProperties props;

    // ---------- Topicos ----------

    public List<TopicInfo> listarTopics() {
        Set<String> nombres = esperar(admin.listTopics().names());
        if (nombres.isEmpty()) return List.of();
        Map<String, TopicDescription> descr = esperar(admin.describeTopics(nombres).allTopicNames());
        Map<String, Map<String, String>> configs = configs(nombres);
        return descr.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(d -> aInfo(d, configs.getOrDefault(d.name(), Map.of())))
                .toList();
    }

    public TopicInfo obtenerTopic(String nombre) {
        Map<String, TopicDescription> d = esperar(admin.describeTopics(List.of(nombre)).allTopicNames());
        return aInfo(d.get(nombre), configs(List.of(nombre)).getOrDefault(nombre, Map.of()));
    }

    public TopicInfo crearTopic(TopicRequest req) {
        NewTopic nuevo = new NewTopic(req.nombre(), req.particiones(), (short) req.factorReplicacion());
        if (req.configuracion() != null && !req.configuracion().isEmpty()) {
            validarConfigs(req.configuracion());
            nuevo.configs(req.configuracion());
        }
        esperar(admin.createTopics(List.of(nuevo)).all());
        log.info("Topico creado: {}", req.nombre());
        return obtenerTopic(req.nombre());
    }

    public TopicInfo actualizarConfig(String nombre, Map<String, String> cambios) {
        validarConfigs(cambios);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Collection<AlterConfigOp> ops = cambios.entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, ops)).all());
        log.info("Configuracion actualizada en {}: {}", nombre, cambios);
        return obtenerTopic(nombre);
    }

    public TopicInfo aumentarParticiones(String nombre, int total) {
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.info("Particiones de {} aumentadas a {}", nombre, total);
        return obtenerTopic(nombre);
    }

    public void eliminarTopic(String nombre) {
        if (protegidos().contains(nombre)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Topico del sistema, no se puede eliminar: " + nombre);
        }
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.info("Topico eliminado: {}", nombre);
    }

    // ---------- Consumer groups ----------

    @SuppressWarnings("removal")
    public List<GrupoInfo> listarGrupos() {
        Collection<ConsumerGroupListing> grupos = esperar(admin.listConsumerGroups().all());
        List<String> ids = grupos.stream().map(ConsumerGroupListing::groupId).sorted().toList();
        if (ids.isEmpty()) return List.of();
        Map<String, ConsumerGroupDescription> d = esperar(admin.describeConsumerGroups(ids).all());
        return ids.stream()
                .map(id -> new GrupoInfo(id, d.get(id).groupState().toString(), d.get(id).members().size()))
                .toList();
    }

    public GrupoDetalle detalleGrupo(String id) {
        ConsumerGroupDescription d = esperar(admin.describeConsumerGroups(List.of(id)).all()).get(id);
        if (d == null || "Dead".equalsIgnoreCase(d.groupState().toString())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consumer group no encontrado: " + id);
        }
        Map<TopicPartition, OffsetAndMetadata> actuales =
                esperar(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata());

        Map<TopicPartition, OffsetSpec> pedir = new HashMap<>();
        actuales.keySet().forEach(tp -> pedir.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales =
                pedir.isEmpty() ? Map.of() : esperar(admin.listOffsets(pedir).all());

        List<ParticionLag> particiones = actuales.entrySet().stream()
                .map(e -> {
                    long fin = finales.get(e.getKey()).offset();
                    long actual = e.getValue().offset();
                    return new ParticionLag(e.getKey().topic(), e.getKey().partition(), actual, fin, Math.max(0L, fin - actual));
                })
                .sorted(Comparator.comparing(ParticionLag::topic).thenComparing(ParticionLag::particion))
                .toList();
        return new GrupoDetalle(id, d.groupState().toString(), d.members().size(), particiones);
    }

    // ---------- Helpers ----------

    private Set<String> protegidos() {
        return Set.of(
                props.entrada().name(),
                props.salida().name(),
                KafkaTopicsProperties.dltDe(props.entrada().name()),
                KafkaTopicsProperties.dltDe(props.salida().name()));
    }

    private void validarConfigs(Map<String, String> configs) {
        configs.forEach((k, v) -> {
            if (!CONFIGS_PERMITIDAS.contains(k)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Configuracion no permitida: " + k + ". Permitidas: " + CONFIGS_PERMITIDAS);
            }
            if (v == null || v.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor vacio para " + k);
            }
        });
    }

    private Map<String, Map<String, String>> configs(Collection<String> topics) {
        List<ConfigResource> recursos = topics.stream()
                .map(t -> new ConfigResource(ConfigResource.Type.TOPIC, t)).toList();
        Map<ConfigResource, Config> res = esperar(admin.describeConfigs(recursos).all());
        Map<String, Map<String, String>> out = new HashMap<>();
        res.forEach((recurso, config) -> {
            Map<String, String> m = new TreeMap<>();
            config.entries().stream()
                    .filter(e -> CONFIGS_PERMITIDAS.contains(e.name()))
                    .forEach(e -> m.put(e.name(), e.value()));
            out.put(recurso.name(), m);
        });
        return out;
    }

    private TopicInfo aInfo(TopicDescription d, Map<String, String> config) {
        int rf = d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
        return new TopicInfo(d.name(), d.partitions().size(), rf, config, protegidos().contains(d.name()));
    }

    private <T> T esperar(KafkaFuture<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Operacion interrumpida");
        } catch (TimeoutException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Kafka no respondio a tiempo");
        } catch (ExecutionException ex) {
            throw traducir(ex.getCause());
        }
    }

    private ResponseStatusException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Topico no encontrado");
        }
        if (causa instanceof TopicExistsException) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "El topico ya existe");
        }
        if (causa instanceof InvalidTopicException || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidRequestException || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidReplicationFactorException) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, causa.getMessage());
        }
        log.error("Error de Kafka", causa);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No se pudo completar la operacion en Kafka: " + causa.getMessage());
    }
}
