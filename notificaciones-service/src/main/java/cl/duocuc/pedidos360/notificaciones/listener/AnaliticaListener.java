package cl.duocuc.pedidos360.notificaciones.listener;

import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import java.math.BigDecimal;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Grupo "analitica": lee los mismos eventos que "notificaciones", pero con su
 * propio offset, asi que recibe todos los mensajes de forma independiente.
 */
@Component
public class AnaliticaListener {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaListener.class);

    private long totalPedidos = 0;
    private BigDecimal montoAcumulado = BigDecimal.ZERO;

    @KafkaListener(
            topics = "pedidos.eventos",
            groupId = "analitica",
            containerFactory = "eventosFactory")
    public synchronized void alRecibirEvento(ConsumerRecord<String, PedidoEventoDTO> registro) {
        PedidoEventoDTO evento = registro.value();
        totalPedidos++;
        montoAcumulado = montoAcumulado.add(evento.total());
        log.info("[analitica] pedidos={} montoAcumulado={} (pedido {} particion={} offset={})",
                totalPedidos, montoAcumulado, evento.pedidoId(), registro.partition(), registro.offset());
    }

    public synchronized long getTotalPedidos() {
        return totalPedidos;
    }

    public synchronized BigDecimal getMontoAcumulado() {
        return montoAcumulado;
    }
}