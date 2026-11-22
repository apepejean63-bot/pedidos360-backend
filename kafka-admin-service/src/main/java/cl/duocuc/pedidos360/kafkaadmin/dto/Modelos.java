package cl.duocuc.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

/** Objetos que entran y salen de la API de administracion. */
public final class Modelos {

    private Modelos() {
    }

    // ---------- Entradas ----------

    public record CrearTopicRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "nombre de topic invalido") String name,
            @Min(1) int partitions,
            @Min(1) short replicationFactor,
            Map<String, String> configs) {
    }

    public record CambiarConfigRequest(@NotBlank String key, @NotBlank String value) {
    }

    public record AmpliarParticionesRequest(@Min(1) int total) {
    }

    // ---------- Salidas: topics ----------

    public record TopicResumen(String name, int partitions, int replicationFactor) {
    }

    public record ParticionInfo(int id, int leader, List<Integer> replicas, List<Integer> isr) {
    }

    public record TopicDetalle(String name, int replicationFactor, List<ParticionInfo> partitions,
                               Map<String, String> configs) {
    }

    // ---------- Salidas: grupos ----------

    public record GrupoResumen(String groupId, String state) {
    }

    public record MiembroGrupo(String consumerId, String clientId, String host, List<String> particiones) {
    }

    public record LagParticion(String topic, int particion, long offsetActual, long offsetFinal, long lag) {
    }

    public record GrupoDetalle(String groupId, String state, String coordinador, List<MiembroGrupo> miembros,
                               long lagTotal, List<LagParticion> particiones) {
    }
}