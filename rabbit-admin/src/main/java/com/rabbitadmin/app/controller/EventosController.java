package com.rabbitadmin.app.controller;

import com.rabbitadmin.app.messaging.event.PedidoEvent;
import com.rabbitadmin.app.messaging.event.StockBajoEvent;
import com.rabbitadmin.app.messaging.event.VentaRegistradaEvent;
import com.rabbitadmin.app.messaging.producer.PedidoEventPublisher;
import com.rabbitadmin.app.messaging.producer.VentasEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventosController {

    private final PedidoEventPublisher pedidoPublisher;
    private final VentasEventPublisher ventasPublisher;

    @PostMapping("/pedidos/creado")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void pedidoCreado(@RequestBody PedidoEvent evento) {
        pedidoPublisher.publicarPedidoCreado(evento);
    }

    @PostMapping("/pedidos/estado-cambiado")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void pedidoEstadoCambiado(@RequestBody PedidoEvent evento) {
        pedidoPublisher.publicarEstadoCambiado(evento);
    }

    @PostMapping("/ventas/registrada")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ventaRegistrada(@RequestBody VentaRegistradaEvent evento) {
        ventasPublisher.publicarVentaRegistrada(evento);
    }

    @PostMapping("/ventas/stock-bajo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void stockBajo(@RequestBody StockBajoEvent evento) {
        ventasPublisher.publicarStockBajo(evento);
    }
}