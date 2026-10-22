package cl.duocuc.pedidos360.pedidos.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PedidoConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoConsumer.class);

    private static final int MAX_INTENTOS = 3;

    @RabbitListener(queues = "${rabbitmq.pedidos.queue}")
    public void procesarPedido(Message message, Channel channel)
            throws IOException {

        long deliveryTag =
                message.getMessageProperties().getDeliveryTag();

        String contenido = new String(
                message.getBody(),
                StandardCharsets.UTF_8
        );

        log.info(
                "Mensaje recibido desde RabbitMQ: {}",
                contenido
        );

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {

            try {

                log.info(
                        "Procesando mensaje. Intento {}/{}",
                        intento,
                        MAX_INTENTOS
                );

                procesarTareaAsincrona(contenido);

                channel.basicAck(deliveryTag, false);

                log.info(
                        "ACK enviado. Mensaje procesado correctamente. DeliveryTag: {}",
                        deliveryTag
                );

                return;

            } catch (Exception e) {

                log.warn(
                        "Error procesando mensaje. Intento {}/{}. Error: {}",
                        intento,
                        MAX_INTENTOS,
                        e.getMessage()
                );

                if (intento == MAX_INTENTOS) {

                    /*
                     * NACK con requeue=false.
                     * pedidos.queue tiene configurado pedidos.dlx,
                     * por lo que RabbitMQ enviara el mensaje
                     * rechazado a pedidos.dlq.
                     */
                    channel.basicNack(
                            deliveryTag,
                            false,
                            false
                    );

                    log.error(
                            "NACK enviado despues de {} intentos. " +
                            "Mensaje enviado a DLQ. DeliveryTag: {}",
                            MAX_INTENTOS,
                            deliveryTag
                    );
                }
            }
        }
    }

    private void procesarTareaAsincrona(String contenido) {

        /*
         * ERROR_TEST se utiliza exclusivamente para demostrar
         * el manejo de errores, reintentos, NACK y DLQ.
         */
        if (contenido.contains("ERROR_TEST")) {
            throw new IllegalStateException(
                    "Error controlado para demostrar DLQ"
            );
        }

        /*
         * Aqui puede ejecutarse una tarea asincrona real:
         * enviar notificacion, generar ticket,
         * generar documento, etc.
         */
    }
}