package cl.duocuc.pedidos360.kafkaadmin.service;

import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.CrearTopicRequest;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.GrupoDetalle;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.GrupoResumen;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.LagParticion;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.MiembroGrupo;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.ParticionInfo;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.TopicDetalle;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.TopicResumen;
import cl.duocuc.pedidos360.kafkaadmin.error.KafkaAdminException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Operaciones de administracion sobre topics y grupos de consumidores. */
@Service
public class KafkaAdminService {

    private static final long TIMEOUT_SEGUNDOS = 15;

    private final Admin admin;

    public KafkaAdminService(Admin admin) {
        this.admin = admin;
    }

    // ---------- Utilidades ----------

    /** Espera la respuesta de Kafka y traduce sus errores a codigos HTTP claros. */
    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException(HttpStatus.SERVICE_UNAVAILABLE, "Operacion interrumpida");
        } catch (TimeoutException e) {
            throw new KafkaAdminException(HttpStatus.GATEWAY_TIMEOUT, "Kafka no respondio a tiempo");
        } catch (ExecutionException e) {
            throw traducir(e.getCause());
        }
    }

    private KafkaAdminException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new KafkaAdminException(HttpStatus.NOT_FOUND, "El topic no existe");
        }
        if (causa instanceof GroupIdNotFoundException) {
            return new KafkaAdminException(HttpStatus.NOT_FOUND, "El grupo no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new KafkaAdminException(HttpStatus.CONFLICT, "El topic ya existe");
        }
        if (causa instanceof InvalidTopicException
                || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidRequestException) {
            return new KafkaAdminException(HttpStatus.BAD_REQUEST, causa.getMessage());
        }
        String detalle = causa == null ? "sin detalle" : causa.getMessage();
        return new KafkaAdminException(HttpStatus.SERVICE_UNAVAILABLE, "Error de Kafka: " + detalle);
    }

    // ---------- Topics ----------

    public List<TopicResumen> listarTopics() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        return descripciones.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(t -> new TopicResumen(t.name(), t.partitions().size(), factorReplicacion(t)))
                .toList();
    }

    private int factorReplicacion(TopicDescription topic) {
        return topic.partitions().isEmpty() ? 0 : topic.partitions().get(0).replicas().size();
    }

    public TopicDetalle describirTopic(String nombre) {
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(List.of(nombre)).allTopicNames());
        TopicDescription topic = descripciones.get(nombre);

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Map<ConfigResource, Config> configuraciones = esperar(admin.describeConfigs(List.of(recurso)).all());

        // Solo las configuraciones que se definieron para este topic (no las por defecto)
        Map<String, String> propias = new TreeMap<>();
        for (ConfigEntry entrada : configuraciones.get(recurso).entries()) {
            if (entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                propias.put(entrada.name(), entrada.value());
            }
        }

        List<ParticionInfo> particiones = new ArrayList<>();
        for (TopicPartitionInfo info : topic.partitions()) {
            particiones.add(new ParticionInfo(
                    info.partition(),
                    info.leader() == null ? -1 : info.leader().id(),
                    info.replicas().stream().map(Node::id).toList(),
                    info.isr().stream().map(Node::id).toList()));
        }
        return new TopicDetalle(nombre, factorReplicacion(topic), particiones, propias);
    }

    public void crearTopic(CrearTopicRequest solicitud) {
        NewTopic nuevo = new NewTopic(solicitud.name(), solicitud.partitions(), solicitud.replicationFactor());
        if (solicitud.configs() != null && !solicitud.configs().isEmpty()) {
            nuevo.configs(solicitud.configs());
        }
        esperar(admin.createTopics(List.of(nuevo)).all());
    }

    public void cambiarConfig(String nombre, String clave, String valor) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        AlterConfigOp operacion = new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET);
        Map<ConfigResource, Collection<AlterConfigOp>> cambios = new HashMap<>();
        cambios.put(recurso, List.of(operacion));
        esperar(admin.incrementalAlterConfigs(cambios).all());
    }

    public void ampliarParticiones(String nombre, int total) {
        Map<String, NewPartitions> cambios = new HashMap<>();
        cambios.put(nombre, NewPartitions.increaseTo(total));
        esperar(admin.createPartitions(cambios).all());
    }

    // ---------- Grupos de consumidores ----------

    public List<GrupoResumen> listarGrupos() {
        Collection<ConsumerGroupListing> grupos = esperar(admin.listConsumerGroups().all());
        return grupos.stream()
                .map(g -> new GrupoResumen(g.groupId(), g.state().map(ConsumerGroupState::name).orElse("DESCONOCIDO")))
                .sorted(Comparator.comparing(GrupoResumen::groupId))
                .toList();
    }

    public GrupoDetalle describirGrupo(String id) {
        Map<String, ConsumerGroupDescription> descripciones =
                esperar(admin.describeConsumerGroups(List.of(id)).all());
        ConsumerGroupDescription grupo = descripciones.get(id);
        if (grupo == null || grupo.state() == ConsumerGroupState.DEAD) {
            throw new KafkaAdminException(HttpStatus.NOT_FOUND, "El grupo no existe");
        }

        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata());

        Set<TopicPartition> particiones = new HashSet<>(confirmados.keySet());
        List<MiembroGrupo> miembros = new ArrayList<>();
        for (MemberDescription miembro : grupo.members()) {
            Set<TopicPartition> asignadas = miembro.assignment().topicPartitions();
            particiones.addAll(asignadas);
            miembros.add(new MiembroGrupo(
                    miembro.consumerId(),
                    miembro.clientId(),
                    miembro.host(),
                    asignadas.stream().map(TopicPartition::toString).sorted().toList()));
        }

        Map<TopicPartition, Long> finales = new HashMap<>();
        if (!particiones.isEmpty()) {
            Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
            for (TopicPartition particion : particiones) {
                consulta.put(particion, OffsetSpec.latest());
            }
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> respuesta =
                    esperar(admin.listOffsets(consulta).all());
            respuesta.forEach((particion, info) -> finales.put(particion, info.offset()));
        }

        List<LagParticion> lag = CalculadorLag.calcular(confirmados, finales);
        long lagTotal = lag.stream().mapToLong(LagParticion::lag).sum();
        Node coordinador = grupo.coordinator();
        String coordinadorTexto = coordinador == null ? "desconocido" : coordinador.host() + ":" + coordinador.port();

        return new GrupoDetalle(id, grupo.state().name(), coordinadorTexto, miembros, lagTotal, lag);
    }
}