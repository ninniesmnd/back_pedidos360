package com.pedidos.app.messaging.consumer;

import com.pedidos.app.messaging.event.ResultadoStreamingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Mantiene en el backend principal los ultimos resultados y alertas procesados por Kafka Streaming. */
@Slf4j
@Service
public class StreamingReaccionService {

    private static final int MAX_EVENTOS = 100;
    private final Deque<ResultadoStreamingEvent> ultimosEventos = new ConcurrentLinkedDeque<>();

    public void procesar(ResultadoStreamingEvent evento) {
        ultimosEventos.addFirst(evento);
        while (ultimosEventos.size() > MAX_EVENTOS) {
            ultimosEventos.pollLast();
        }
        if ("STOCK_BAJO".equals(evento.tipo())) {
            log.warn("[STREAMING-ALERTA] local={} producto={} stockActual={}",
                    evento.localId(), evento.clave(), evento.valor());
        } else {
            log.info("[STREAMING-METRICA] tipo={} local={} clave={} valor={} ventana=[{}..{}]",
                    evento.tipo(), evento.localId(), evento.clave(), evento.valor(),
                    evento.ventanaInicio(), evento.ventanaFin());
        }
    }

    public List<ResultadoStreamingEvent> listarPorLocal(Long localId) {
        if (localId == null) {
            return List.copyOf(ultimosEventos);
        }
        return ultimosEventos.stream()
                .filter(e -> localId.equals(e.localId()))
                .toList();
    }
}
