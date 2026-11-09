package com.kafkaadmin.app.controller;

import com.kafkaadmin.app.dto.KafkaAdminDtos.*;
import com.kafkaadmin.app.service.KafkaAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaAdminController {

    private final KafkaAdminService service;

    @GetMapping("/topics")
    public List<TopicInfo> listar() { return service.listarTopics(); }

    @GetMapping("/topics/{nombre}")
    public TopicInfo obtener(@PathVariable String nombre) { return service.obtenerTopic(nombre); }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicInfo crear(@Valid @RequestBody TopicRequest request) { return service.crearTopic(request); }

    @PatchMapping("/topics/{nombre}/config")
    public TopicInfo actualizarConfig(@PathVariable String nombre, @Valid @RequestBody ConfigRequest request) {
        return service.actualizarConfig(nombre, request.configuracion());
    }

    @PutMapping("/topics/{nombre}/particiones")
    public TopicInfo aumentarParticiones(@PathVariable String nombre, @Valid @RequestBody ParticionesRequest request) {
        return service.aumentarParticiones(nombre, request.total());
    }

    @DeleteMapping("/topics/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombre) { service.eliminarTopic(nombre); }

    @GetMapping("/consumer-groups")
    public List<GrupoInfo> grupos() { return service.listarGrupos(); }

    @GetMapping("/consumer-groups/{id}")
    public GrupoDetalle grupo(@PathVariable String id) { return service.detalleGrupo(id); }
}
