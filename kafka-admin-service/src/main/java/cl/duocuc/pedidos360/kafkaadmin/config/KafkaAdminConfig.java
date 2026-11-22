package cl.duocuc.pedidos360.kafkaadmin.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Crea el cliente de administracion de Kafka (AdminClient) que usa todo el servicio. */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public Admin clienteAdmin(
            @Value("${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093,localhost:9094}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 15000);
        props.put(AdminClientConfig.CLIENT_ID_CONFIG, "kafka-admin-service");
        return Admin.create(props);
    }
}