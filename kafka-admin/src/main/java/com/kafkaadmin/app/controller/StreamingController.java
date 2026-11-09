package com.kafkaadmin.app.controller;

import com.kafkaadmin.app.messaging.event.ResultadoEvent;
import com.kafkaadmin.app.messaging.resultados.ResultadosService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class StreamingController {

    private final ResultadosService service;

    @GetMapping("/analitica/resultados")
    public List<ResultadoEvent> resultados() { return service.resultados(); }

    @GetMapping("/alertas")
    public List<ResultadoEvent> alertas() { return service.alertas(); }
}
