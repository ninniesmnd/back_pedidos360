package com.kafkastreaming.app.controller;

import com.kafkastreaming.app.config.StreamingProperties;
import com.kafkastreaming.app.runner.StreamingRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor
public class StatusController {

    private final StreamingRunner runner;
    private final StreamingProperties props;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "applicationId", props.applicationId(),
                "entrada", props.entrada(),
                "salida", props.salida(),
                "threads", props.threads(),
                "ventanaSegundos", props.ventanaSegundos(),
                "state", runner.estado()
        );
    }
}
