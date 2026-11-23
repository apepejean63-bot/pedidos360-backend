package cl.duocuc.pedidos360.pedidos.messaging;

import cl.duocuc.pedidos360.pedidos.config.RabbitMQTopicConfig;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoTopicListeners {

    private static final Logger log = LoggerFactory.getLogger(PedidoTopicListeners.class);

    @RabbitListener(queues = RabbitMQTopicConfig.COLA_NOTIFICACIONES)
    public void notificar(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String contenido = new String(message.getBody(), StandardCharsets.UTF_8);

        if (contenido.contains("ERROR_TEST")) {
            // requeue=false: la cola tiene DLX, el mensaje va a notificaciones.dlq
            channel.basicNack(tag, false, false);
            log.error("[notificaciones.queue] NACK. Mensaje enviado a notificaciones.dlq");
            return;
        }
        log.info("[notificaciones.queue] Notificacion procesada: {}", contenido);
        channel.basicAck(tag, false);
    }

    @RabbitListener(queues = RabbitMQTopicConfig.COLA_AUDITORIA)
    public void auditar(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String contenido = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("[auditoria.queue] Evento auditado (routingKey={}): {}",
                message.getMessageProperties().getReceivedRoutingKey(), contenido);
        channel.basicAck(tag, false);
    }
}