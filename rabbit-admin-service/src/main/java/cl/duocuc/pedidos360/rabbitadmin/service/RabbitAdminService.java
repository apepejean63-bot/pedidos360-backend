package cl.duocuc.pedidos360.rabbitadmin.service;

import cl.duocuc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.QueueRequest;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void crearCola(QueueRequest request) {

        Queue queue = new Queue(
                request.getName(),
                true,
                false,
                false
        );

        amqpAdmin.declareQueue(queue);
    }

    public boolean eliminarCola(String name) {
        return amqpAdmin.deleteQueue(name);
    }

    public void crearExchange(ExchangeRequest request) {

        DirectExchange exchange = new DirectExchange(
                request.getName(),
                true,
                false
        );

        amqpAdmin.declareExchange(exchange);
    }

    public boolean eliminarExchange(String name) {
        return amqpAdmin.deleteExchange(name);
    }

    public void crearBinding(BindingRequest request) {

        Binding binding = new Binding(
                request.getQueueName(),
                Binding.DestinationType.QUEUE,
                request.getExchangeName(),
                request.getRoutingKey(),
                null
        );

        amqpAdmin.declareBinding(binding);
    }

    public void eliminarBinding(BindingRequest request) {

        Binding binding = new Binding(
                request.getQueueName(),
                Binding.DestinationType.QUEUE,
                request.getExchangeName(),
                request.getRoutingKey(),
                null
        );

        amqpAdmin.removeBinding(binding);
    }
}