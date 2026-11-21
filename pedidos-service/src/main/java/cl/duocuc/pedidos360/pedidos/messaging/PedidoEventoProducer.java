package cl.duocuc.pedidos360.pedidos.messaging;

import cl.duocuc.pedidos360.pedidos.config.KafkaConfig;
import cl.duocuc.pedidos360.pedidos.entity.DetallePedido;
import cl.duocuc.pedidos360.pedidos.entity.Pedido;
import cl.duocuc.pedidos360.pedidos.event.PedidoEstadoDTO;
import cl.duocuc.pedidos360.pedidos.event.PedidoEventoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class PedidoEventoProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PedidoEventoProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarCreado(Pedido pedido) {
        publicar(pedido, "PEDIDO_CREADO");
    }

    public void publicarEstadoCambiado(Pedido pedido) {
        publicar(pedido, "ESTADO_CAMBIADO");
    }

    private void publicar(Pedido pedido, String tipo) {
        try {
            String clave = String.valueOf(pedido.getId());
            String ahora = Instant.now().toString();

            PedidoEventoDTO evento = new PedidoEventoDTO(
                    UUID.randomUUID().toString(),
                    tipo,
                    pedido.getId(),
                    pedido.getCliente(),
                    pedido.getEmail(),
                    pedido.getEstado().name(),
                    calcularTotal(pedido),
                    ahora,
                    1);

            PedidoEstadoDTO estado = new PedidoEstadoDTO(
                    pedido.getId(),
                    pedido.getEstado().name(),
                    ahora);

            enviar(KafkaConfig.TOPIC_EVENTOS, clave, evento);
            enviar(KafkaConfig.TOPIC_ESTADO, clave, estado);
        } catch (Exception e) {
            // Un fallo de Kafka no debe impedir que el pedido se cree o cambie de estado
            log.error("No se pudo publicar en Kafka el evento {} del pedido {}: {}",
                    tipo, pedido.getId(), e.getMessage());
        }
    }

    private void enviar(String topico, String clave, Object valor) {
        kafkaTemplate.send(topico, clave, valor).whenComplete((resultado, error) -> {
            if (error != null) {
                log.error("Fallo al enviar a Kafka topico={} clave={}: {}", topico, clave, error.getMessage());
            } else {
                log.info("Evento publicado en Kafka topico={} clave={} particion={} offset={}",
                        topico, clave,
                        resultado.getRecordMetadata().partition(),
                        resultado.getRecordMetadata().offset());
            }
        });
    }

    private BigDecimal calcularTotal(Pedido pedido) {
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido d : pedido.getDetalles()) {
            BigDecimal precio = new BigDecimal(String.valueOf(d.getPrecioUnitario()));
            BigDecimal cantidad = new BigDecimal(String.valueOf(d.getCantidad()));
            total = total.add(precio.multiply(cantidad));
        }
        return total;
    }
}