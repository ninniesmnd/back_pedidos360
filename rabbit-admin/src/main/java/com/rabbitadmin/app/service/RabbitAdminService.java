package com.rabbitadmin.app.service;

import com.rabbitadmin.app.dto.BindingRequest;
import com.rabbitadmin.app.dto.ExchangeRequest;
import com.rabbitadmin.app.dto.QueueRequest;
import com.rabbitadmin.app.exception.RecursoNoEncontradoException;
import com.rabbitadmin.app.exception.RecursoProtegidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final Set<String> protegidos;

    public RabbitAdminService(AmqpAdmin amqpAdmin,
                              @Value("${app.rabbit-admin.protegidos}") List<String> protegidos) {
        this.amqpAdmin = amqpAdmin;
        this.protegidos = Set.copyOf(protegidos);
    }

    // ---- Colas ----
    public void crearCola(QueueRequest req) {
        validarNoProtegido(req.name());
        boolean durable = req.durable() == null || req.durable();
        QueueBuilder builder = durable ? QueueBuilder.durable(req.name()) : QueueBuilder.nonDurable(req.name());
        if (StringUtils.hasText(req.deadLetterExchange())) {
            builder.deadLetterExchange(req.deadLetterExchange());
        }
        if (StringUtils.hasText(req.deadLetterRoutingKey())) {
            builder.deadLetterRoutingKey(req.deadLetterRoutingKey());
        }
        amqpAdmin.declareQueue(builder.build());
        log.info("Cola creada: {} (durable={})", req.name(), durable);
    }

    public QueueInformation infoCola(String nombre) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return info;
    }

    public void eliminarCola(String nombre) {
        validarNoProtegido(nombre);
        infoCola(nombre); // lanza 404 si no existe
        amqpAdmin.deleteQueue(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    // ---- Exchanges ----
    public void crearExchange(ExchangeRequest req) {
        validarNoProtegido(req.name());
        boolean durable = req.durable() == null || req.durable();
        ExchangeBuilder builder = switch (req.type().toLowerCase()) {
            case "direct" -> ExchangeBuilder.directExchange(req.name());
            case "topic" -> ExchangeBuilder.topicExchange(req.name());
            case "fanout" -> ExchangeBuilder.fanoutExchange(req.name());
            case "headers" -> ExchangeBuilder.headersExchange(req.name());
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + req.type());
        };
        amqpAdmin.declareExchange(builder.durable(durable).build());
        log.info("Exchange creado: {} ({})", req.name(), req.type());
    }

    public void eliminarExchange(String nombre) {
        validarNoProtegido(nombre);
        amqpAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // ---- Bindings ----
    public void crearBinding(BindingRequest req) {
        validarNoProtegido(req.queue());
        validarNoProtegido(req.exchange());
        amqpAdmin.declareBinding(construirBinding(req));
        log.info("Binding creado: {} -> {} ({})", req.exchange(), req.queue(), req.routingKey());
    }

    public void eliminarBinding(BindingRequest req) {
        validarNoProtegido(req.queue());
        validarNoProtegido(req.exchange());
        amqpAdmin.removeBinding(construirBinding(req));
        log.info("Binding eliminado: {} -> {} ({})", req.exchange(), req.queue(), req.routingKey());
    }

    private Binding construirBinding(BindingRequest req) {
        String routingKey = req.routingKey() == null ? "" : req.routingKey();
        return new Binding(req.queue(), Binding.DestinationType.QUEUE, req.exchange(), routingKey, null);
    }

    private void validarNoProtegido(String nombre) {
        if (nombre.startsWith("amq.") || protegidos.contains(nombre)) {
            throw new RecursoProtegidoException(nombre);
        }
    }
}