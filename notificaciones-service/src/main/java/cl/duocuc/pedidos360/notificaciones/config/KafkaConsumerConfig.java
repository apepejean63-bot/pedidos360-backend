package cl.duocuc.pedidos360.notificaciones.config;

import cl.duocuc.pedidos360.notificaciones.error.EventoInvalidoException;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEstadoDTO;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuracion de los consumidores y del manejo de errores.
 * Cada tipo de mensaje (DTO) tiene su propia fabrica de listeners.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    private static final String PAQUETE_EVENTOS = "cl.duocuc.pedidos360.notificaciones.event";

    @Value("${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093,localhost:9094}")
    private String bootstrapServers;

    /** Hilos consumidores del grupo "notificaciones": 3 = uno por particion. */
    @Value("${notificaciones.concurrency:3}")
    private int concurrenciaNotificaciones;

    // ---------- Topics de mensajes fallidos (DLT) ----------

    @Bean
    public NewTopic eventosDlt() {
        return TopicBuilder.name("pedidos.eventos.DLT")
                .partitions(3)
                .replicas(3)
                .config("min.insync.replicas", "2")
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic estadoDlt() {
        return TopicBuilder.name("pedidos.estado.DLT")
                .partitions(3)
                .replicas(3)
                .config("min.insync.replicas", "2")
                .config("retention.ms", "604800000")
                .build();
    }

    // ---------- Publicacion en la DLT ----------

    private Map<String, Object> propiedadesProductor() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        return props;
    }

    /** Para mensajes que se leyeron bien (DTO) pero fallaron al procesarse. */
    @Bean
    public KafkaTemplate<String, Object> plantillaDltJson() {
        Map<String, Object> props = propiedadesProductor();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<String, Object>(new DefaultKafkaProducerFactory<String, Object>(props));
    }

    /** Para mensajes que no se pudieron leer (JSON roto): se reenvian tal cual llegaron. */
    @Bean
    public KafkaTemplate<String, byte[]> plantillaDltBytes() {
        Map<String, Object> props = propiedadesProductor();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new KafkaTemplate<String, byte[]>(new DefaultKafkaProducerFactory<String, byte[]>(props));
    }

    @Bean
    public DeadLetterPublishingRecoverer recuperadorDlt() {
        Map<Class<?>, KafkaOperations<? extends Object, ? extends Object>> plantillas = new LinkedHashMap<>();
        plantillas.put(byte[].class, plantillaDltBytes());
        plantillas.put(Object.class, plantillaDltJson());
        // Mismo nombre del topic original + ".DLT", en la misma particion
        return new DeadLetterPublishingRecoverer(plantillas,
                (registro, error) -> new TopicPartition(registro.topic() + ".DLT", registro.partition()));
    }

    // ---------- Manejadores de errores ----------

    private static String causa(Exception error) {
        Throwable raiz = error.getCause() != null ? error.getCause() : error;
        return raiz.getClass().getSimpleName() + ": " + raiz.getMessage();
    }

    /** 1 intento + 3 reintentos (1 s entre cada uno). Si sigue fallando, el mensaje va a la DLT. */
    private DefaultErrorHandler manejadorConDlt() {
        DeadLetterPublishingRecoverer dlt = recuperadorDlt();
        ConsumerRecordRecoverer conLog = (registro, error) -> {
            log.error("[error] Mensaje enviado a la DLT: topico={} particion={} offset={} clave={} causa={}",
                    registro.topic(), registro.partition(), registro.offset(), registro.key(), causa(error));
            dlt.accept(registro, error);
        };
        DefaultErrorHandler manejador = new DefaultErrorHandler(conLog, new FixedBackOff(1000L, 3L));
        // Un mensaje con datos invalidos no mejora si se reintenta: va directo a la DLT
        manejador.addNotRetryableExceptions(EventoInvalidoException.class);
        return manejador;
    }

    /** Sin reintentos ni DLT: registra el error y sigue con el siguiente mensaje. */
    private DefaultErrorHandler manejadorSoloLog() {
        DefaultErrorHandler manejador = new DefaultErrorHandler(new FixedBackOff(0L, 0L));
        manejador.addNotRetryableExceptions(EventoInvalidoException.class);
        return manejador;
    }

    // ---------- Consumidores ----------

    private <T> ConsumerFactory<String, T> fabricaConsumidor(Class<T> tipo) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Un grupo nuevo empieza desde el primer mensaje del topic
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // El offset se confirma despues de procesar, no antes
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<String, T>(
                props,
                () -> new StringDeserializer(),
                () -> {
                    // false: no depende de cabeceras de tipo, usa siempre el DTO indicado
                    JsonDeserializer<T> json = new JsonDeserializer<>(tipo, false);
                    json.addTrustedPackages(PAQUETE_EVENTOS);
                    // Si el JSON esta roto no se cae el consumidor: el error llega al manejador
                    return new ErrorHandlingDeserializer<T>(json);
                });
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> fabricaListener(
            Class<T> tipo, int concurrencia, CommonErrorHandler manejador) {
        ConcurrentKafkaListenerContainerFactory<String, T> fabrica = new ConcurrentKafkaListenerContainerFactory<>();
        fabrica.setConsumerFactory(fabricaConsumidor(tipo));
        fabrica.setConcurrency(concurrencia);
        fabrica.setCommonErrorHandler(manejador);
        return fabrica;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventoDTO> eventosNotificacionesFactory() {
        return fabricaListener(PedidoEventoDTO.class, concurrenciaNotificaciones, manejadorConDlt());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventoDTO> eventosFactory() {
        return fabricaListener(PedidoEventoDTO.class, 1, manejadorSoloLog());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEstadoDTO> estadoFactory() {
        return fabricaListener(PedidoEstadoDTO.class, 1, manejadorConDlt());
    }
}