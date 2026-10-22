package cl.duocuc.pedidos360.pedidos.messaging;

import cl.duocuc.pedidos360.pedidos.entity.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PedidoProducer {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.pedidos.exchange}")
    private String exchange;

    @Value("${rabbitmq.pedidos.routing-key}")
    private String routingKey;

    public PedidoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPedidoCreado(Pedido pedido) {

        String mensaje = String.format(
                "Pedido creado - ID: %d, Cliente: %s, Email: %s, Estado: %s",
                pedido.getId(),
                pedido.getCliente(),
                pedido.getEmail(),
                pedido.getEstado()
        );

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                mensaje
        );

        log.info(
                "Mensaje RabbitMQ publicado. Pedido ID: {}",
                pedido.getId()
        );
    }
}