package cl.duocuc.pedidos360.notificaciones.listener;

import cl.duocuc.pedidos360.notificaciones.error.ServicioCorreoNoDisponibleException;
import cl.duocuc.pedidos360.notificaciones.error.ValidadorEventos;
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
        ValidadorEventos.validarEvento(registro);
        PedidoEventoDTO evento = registro.value();

        // Simulacion para la demo: los correos @falla.test representan una caida del proveedor de correo
        if (evento.email().endsWith("@falla.test")) {
            log.warn("[notificaciones] Proveedor de correo no disponible para pedido {} (particion={} offset={})",
                    evento.pedidoId(), registro.partition(), registro.offset());
            throw new ServicioCorreoNoDisponibleException(
                    "Proveedor de correo no disponible (simulado) para el pedido " + evento.pedidoId());
        }

        log.info("[notificaciones] Enviando correo a {} por pedido {} ({}) hilo={} particion={} offset={}",
                evento.email(), evento.pedidoId(), evento.tipo(),
                Thread.currentThread().getName(), registro.partition(), registro.offset());
    }
}