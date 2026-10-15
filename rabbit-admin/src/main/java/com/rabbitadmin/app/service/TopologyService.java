package com.rabbitadmin.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class TopologyService {

    private final AmqpAdmin amqpAdmin;
    private final Map<String, Declarables> dominios;

    public TopologyService(AmqpAdmin amqpAdmin,
                           @Qualifier("pedidosTopology") Declarables pedidos,
                           @Qualifier("ventasTopology") Declarables ventas) {
        this.amqpAdmin = amqpAdmin;
        this.dominios = Map.of("pedidos", pedidos, "ventas", ventas);
    }

    public int aprovisionar(String dominio) {
        Declarables topologia = dominios.get(dominio == null ? "" : dominio.toLowerCase());
        if (topologia == null) {
            throw new IllegalArgumentException("Dominio desconocido: '" + dominio + "' (use pedidos o ventas)");
        }
        for (Declarable d : topologia.getDeclarables()) {
            if (d instanceof Exchange e) amqpAdmin.declareExchange(e);
            else if (d instanceof Queue q) amqpAdmin.declareQueue(q);
            else if (d instanceof Binding b) amqpAdmin.declareBinding(b);
        }
        log.info("Topologia aprovisionada: dominio={} ({} recursos)", dominio, topologia.getDeclarables().size());
        return topologia.getDeclarables().size();
    }
}