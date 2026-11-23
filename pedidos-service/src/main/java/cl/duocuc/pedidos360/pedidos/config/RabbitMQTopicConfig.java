package cl.duocuc.pedidos360.pedidos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange topic y dos colas adicionales, cada una con su DLQ.
 * Con la cola pedidos.queue (exchange direct) suman las 3 colas del sistema.
 */
@Configuration
public class RabbitMQTopicConfig {

    public static final String TOPIC_EXCHANGE = "pedidos.topic";
    public static final String COLA_NOTIFICACIONES = "notificaciones.queue";
    public static final String COLA_AUDITORIA = "auditoria.queue";
    public static final String DLQ_NOTIFICACIONES = "notificaciones.dlq";
    public static final String DLQ_AUDITORIA = "auditoria.dlq";

    @Value("${rabbitmq.pedidos.dlx}")
    private String deadLetterExchangeName;

    @Bean
    public TopicExchange pedidosTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(COLA_NOTIFICACIONES)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(DLQ_NOTIFICACIONES)
                .build();
    }

    @Bean
    public Queue auditoriaQueue() {
        return QueueBuilder.durable(COLA_AUDITORIA)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(DLQ_AUDITORIA)
                .build();
    }

    @Bean
    public Queue notificacionesDeadLetterQueue() {
        return QueueBuilder.durable(DLQ_NOTIFICACIONES).build();
    }

    @Bean
    public Queue auditoriaDeadLetterQueue() {
        return QueueBuilder.durable(DLQ_AUDITORIA).build();
    }

    // Patron "pedido.*": recibe pedido.creado, pedido.cancelado, etc.
    @Bean
    public Binding bindingNotificaciones() {
        return BindingBuilder.bind(notificacionesQueue()).to(pedidosTopicExchange()).with("pedido.*");
    }

    // Patron "pedido.#": recibe todo lo que empiece con pedido.
    @Bean
    public Binding bindingAuditoria() {
        return BindingBuilder.bind(auditoriaQueue()).to(pedidosTopicExchange()).with("pedido.#");
    }

    @Bean
    public Binding bindingDlqNotificaciones(
            @Qualifier("pedidosDeadLetterExchange") DirectExchange dlx) {
        return BindingBuilder.bind(notificacionesDeadLetterQueue()).to(dlx).with(DLQ_NOTIFICACIONES);
    }

    @Bean
    public Binding bindingDlqAuditoria(
            @Qualifier("pedidosDeadLetterExchange") DirectExchange dlx) {
        return BindingBuilder.bind(auditoriaDeadLetterQueue()).to(dlx).with(DLQ_AUDITORIA);
    }
}