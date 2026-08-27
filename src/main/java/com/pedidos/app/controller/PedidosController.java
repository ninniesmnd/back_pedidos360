package com.pedidos.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidosController {

    @GetMapping("/cocina/ping")
    public String pingCocina() {
        return "OK - autenticado como OperadorCocina o superior";
    }
}