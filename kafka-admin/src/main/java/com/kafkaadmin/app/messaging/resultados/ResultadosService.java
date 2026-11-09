package com.kafkaadmin.app.messaging.resultados;

import com.kafkaadmin.app.messaging.event.ResultadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Guarda en memoria los ultimos resultados del streaming y las alertas de stock. */
@Slf4j
@Service
public class ResultadosService {

    private static final int MAX_ALERTAS = 50;
    private static final int MAX_RESULTADOS = 500;

    private final Map<String, ResultadoEvent> ultimos = new ConcurrentHashMap<>();
    private final Deque<ResultadoEvent> alertas = new ConcurrentLinkedDeque<>();

    public void registrar(ResultadoEvent e) {
        if ("STOCK_BAJO".equals(e.tipo())) {
            alertas.addFirst(e);
            while (alertas.size() > MAX_ALERTAS) alertas.pollLast();
            log.info("[ALERTA-STOCK] local={} {} (stock {})", e.localId(), e.clave(), e.valor());
            return;
        }
        ultimos.put(e.tipo() + "|" + e.localId() + "|" + e.clave() + "|" + e.ventanaInicio(), e);
        if (ultimos.size() > MAX_RESULTADOS) {
            ultimos.entrySet().stream()
                    .min(Comparator.comparing(en -> String.valueOf(en.getValue().ventanaInicio())))
                    .ifPresent(en -> ultimos.remove(en.getKey()));
        }
    }

    public List<ResultadoEvent> resultados() {
        return ultimos.values().stream()
                .sorted(Comparator.comparing((ResultadoEvent r) -> String.valueOf(r.generadoEn())).reversed())
                .limit(100)
                .toList();
    }

    public List<ResultadoEvent> alertas() {
        return List.copyOf(alertas);
    }
}
