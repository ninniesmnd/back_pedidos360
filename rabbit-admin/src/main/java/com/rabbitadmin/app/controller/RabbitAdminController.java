package com.rabbitadmin.app.controller;

import com.rabbitadmin.app.dto.BindingRequest;
import com.rabbitadmin.app.dto.ExchangeRequest;
import com.rabbitadmin.app.dto.QueueRequest;
import com.rabbitadmin.app.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rabbit")
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService service;

    // ---- Colas ----
    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> crearCola(@Valid @RequestBody QueueRequest request) {
        service.crearCola(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("queue", request.name()));
    }

    @GetMapping("/queues/{name}")
    public Map<String, Object> infoCola(@PathVariable String name) {
        QueueInformation info = service.infoCola(name);
        return Map.of("name", info.getName(),
                "messages", info.getMessageCount(),
                "consumers", info.getConsumerCount());
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    // ---- Exchanges ----
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        service.crearExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("exchange", request.name()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ---- Bindings ----
    @PostMapping("/bindings")
    public ResponseEntity<Void> crearBinding(@Valid @RequestBody BindingRequest request) {
        service.crearBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest request) {
        service.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}