package cl.duocuc.pedidos360.rabbitadmin.service;

import cl.duocuc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.QueueRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void crearCola(QueueRequest request) {
        Map<String, Object> argumentos = new HashMap<>();
        if (request.getDeadLetterExchange() != null && !request.getDeadLetterExchange().isBlank()) {
            argumentos.put("x-dead-letter-exchange", request.getDeadLetterExchange());
            if (request.getDeadLetterRoutingKey() != null && !request.getDeadLetterRoutingKey().isBlank()) {
                argumentos.put("x-dead-letter-routing-key", request.getDeadLetterRoutingKey());
            }
        }
        amqpAdmin.declareQueue(new Queue(request.getName(), true, false, false, argumentos));
    }

    public boolean eliminarCola(String name) {
        return amqpAdmin.deleteQueue(name);
    }

    public void crearExchange(ExchangeRequest request) {
        Exchange exchange = switch (request.getType()) {
            case "topic" -> new TopicExchange(request.getName(), true, false);
            case "fanout" -> new FanoutExchange(request.getName(), true, false);
            default -> new DirectExchange(request.getName(), true, false);
        };
        amqpAdmin.declareExchange(exchange);
    }

    public boolean eliminarExchange(String name) {
        return amqpAdmin.deleteExchange(name);
    }

    public void crearBinding(BindingRequest request) {
        amqpAdmin.declareBinding(nuevoBinding(request));
    }

    public void eliminarBinding(BindingRequest request) {
        amqpAdmin.removeBinding(nuevoBinding(request));
    }

    private Binding nuevoBinding(BindingRequest request) {
        return new Binding(
                request.getQueueName(),
                Binding.DestinationType.QUEUE,
                request.getExchangeName(),
                request.getRoutingKey(),
                null);
    }
}