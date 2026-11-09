package com.kafkaadmin.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

public final class KafkaAdminDtos {
    private KafkaAdminDtos() {}

    public record TopicRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "Nombre de topico invalido") String nombre,
            @Min(1) @Max(12) int particiones,
            @Min(1) @Max(3) int factorReplicacion,
            Map<String, String> configuracion) {}

    public record ConfigRequest(@NotEmpty Map<String, String> configuracion) {}

    public record ParticionesRequest(@Min(1) @Max(24) int total) {}

    public record TopicInfo(String nombre, int particiones, int factorReplicacion,
                            Map<String, String> configuracion, boolean protegido) {}

    public record GrupoInfo(String id, String estado, int miembros) {}

    public record ParticionLag(String topic, int particion, long offsetActual, long offsetFinal, long lag) {}

    public record GrupoDetalle(String id, String estado, int miembros, List<ParticionLag> particiones) {}
}
