package cl.duocuc.pedidos360.kafkaadmin.controller;

import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.AmpliarParticionesRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.CambiarConfigRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.CrearTopicRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.GrupoDetalle;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.GrupoResumen;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.TopicDetalle;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.TopicResumen;
import cl.duocuc.pedidos360.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService servicio;

    public KafkaAdminController(KafkaAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/topics")
    public List<TopicResumen> listarTopics() {
        return servicio.listarTopics();
    }

    @GetMapping("/topics/{nombre}")
    public TopicDetalle describirTopic(@PathVariable String nombre) {
        return servicio.describirTopic(nombre);
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicResumen> crearTopic(@Valid @RequestBody CrearTopicRequest solicitud) {
        servicio.crearTopic(solicitud);
        TopicResumen creado = new TopicResumen(solicitud.name(), solicitud.partitions(), solicitud.replicationFactor());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/topics/{nombre}/config")
    public Map<String, String> cambiarConfig(@PathVariable String nombre,
                                             @Valid @RequestBody CambiarConfigRequest solicitud) {
        servicio.cambiarConfig(nombre, solicitud.key(), solicitud.value());
        return Map.of("topic", nombre, "config", solicitud.key(), "valor", solicitud.value());
    }

    @PostMapping("/topics/{nombre}/partitions")
    public Map<String, Object> ampliarParticiones(@PathVariable String nombre,
                                                  @Valid @RequestBody AmpliarParticionesRequest solicitud) {
        servicio.ampliarParticiones(nombre, solicitud.total());
        return Map.of("topic", nombre, "particiones", solicitud.total());
    }

    @GetMapping("/groups")
    public List<GrupoResumen> listarGrupos() {
        return servicio.listarGrupos();
    }

    @GetMapping("/groups/{id}")
    public GrupoDetalle describirGrupo(@PathVariable String id) {
        return servicio.describirGrupo(id);
    }
}