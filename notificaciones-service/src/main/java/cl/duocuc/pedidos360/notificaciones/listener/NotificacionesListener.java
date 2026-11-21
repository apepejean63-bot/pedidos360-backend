package cl.duocuc.pedidos360.notificaciones.listener;

import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Grupo "notificaciones": cada hilo atiende una particion de pedidos.eventos. */
@Component
public class NotificacionesListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesListener.class);

    @KafkaListener(
            topics = "pedidos.eventos",
            groupId = "notificaciones",
            containerFactory = "eventosNotificacionesFactory")
    public void alRecibirEvento(ConsumerRecord<String, PedidoEventoDTO> registro) {
        PedidoEventoDTO evento = registro.value();
        log.info("[notificaciones] Enviando correo a {} por pedido {} ({}) hilo={} particion={} offset={}",
                evento.email(), evento.pedidoId(), evento.tipo(),
                Thread.currentThread().getName(), registro.partition(), registro.offset());
    }
}