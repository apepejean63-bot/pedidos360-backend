package cl.duocuc.pedidos360.notificaciones.config;

import cl.duocuc.pedidos360.notificaciones.event.PedidoEstadoDTO;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Configuracion de los consumidores. Cada tipo de mensaje (DTO) tiene su propia
 * fabrica de listeners, para que el JSON se convierta directo al DTO correcto.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final String PAQUETE_EVENTOS = "cl.duocuc.pedidos360.notificaciones.event";

    @Value("${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093,localhost:9094}")
    private String bootstrapServers;

    /** Hilos consumidores del grupo "notificaciones": 3 = uno por particion. */
    @Value("${notificaciones.concurrency:3}")
    private int concurrenciaNotificaciones;

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
                    return json;
                });
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> fabricaListener(Class<T> tipo, int concurrencia) {
        ConcurrentKafkaListenerContainerFactory<String, T> fabrica = new ConcurrentKafkaListenerContainerFactory<>();
        fabrica.setConsumerFactory(fabricaConsumidor(tipo));
        fabrica.setConcurrency(concurrencia);
        return fabrica;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventoDTO> eventosNotificacionesFactory() {
        return fabricaListener(PedidoEventoDTO.class, concurrenciaNotificaciones);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEventoDTO> eventosFactory() {
        return fabricaListener(PedidoEventoDTO.class, 1);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEstadoDTO> estadoFactory() {
        return fabricaListener(PedidoEstadoDTO.class, 1);
    }
}