package cl.duocuc.pedidos360.pedidos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.pedidos.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.pedidos.queue}")
    private String queueName;

    @Value("${rabbitmq.pedidos.routing-key}")
    private String routingKey;

    @Value("${rabbitmq.pedidos.dlx}")
    private String deadLetterExchangeName;

    @Value("${rabbitmq.pedidos.dlq}")
    private String deadLetterQueueName;

    @Value("${rabbitmq.pedidos.dlq-routing-key}")
    private String deadLetterRoutingKey;

    // Exchange principal
    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    // Dead Letter Exchange
    @Bean
    public DirectExchange pedidosDeadLetterExchange() {
        return new DirectExchange(deadLetterExchangeName, true, false);
    }

    // Cola principal asociada al DLX
    @Bean
    public Queue pedidosQueue() {
        return QueueBuilder
                .durable(queueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    // Dead Letter Queue
    @Bean
    public Queue pedidosDeadLetterQueue() {
        return QueueBuilder
                .durable(deadLetterQueueName)
                .build();
    }

    // Binding: exchange principal -> cola principal
    @Bean
    public Binding pedidosBinding() {
        return BindingBuilder
                .bind(pedidosQueue())
                .to(pedidosExchange())
                .with(routingKey);
    }

    // Binding: DLX -> DLQ
    @Bean
    public Binding pedidosDeadLetterBinding() {
        return BindingBuilder
                .bind(pedidosDeadLetterQueue())
                .to(pedidosDeadLetterExchange())
                .with(deadLetterRoutingKey);
    }
}