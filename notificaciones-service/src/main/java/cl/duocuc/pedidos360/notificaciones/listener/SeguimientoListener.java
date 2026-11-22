package cl.duocuc.pedidos360.notificaciones.listener;

import cl.duocuc.pedidos360.notificaciones.error.ValidadorEventos;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEstadoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Grupo "seguimiento": lee el ultimo estado de cada pedido desde pedidos.estado. */
@Component
public class SeguimientoListener {

    private static final Logger log = LoggerFactory.getLogger(SeguimientoListener.class);

    @KafkaListener(
            topics = "pedidos.estado",
            groupId = "seguimiento",
            containerFactory = "estadoFactory")
    public void alRecibirEstado(ConsumerRecord<String, PedidoEstadoDTO> registro) {
        if (registro.value() == null) {
            // En un topic compactado, un mensaje sin valor borra esa clave (tombstone)
            log.info("[seguimiento] Clave {} eliminada (tombstone) particion={} offset={}",
                    registro.key(), registro.partition(), registro.offset());
            return;
        }
        ValidadorEventos.validarEstado(registro);
        PedidoEstadoDTO estado = registro.value();
        log.info("[seguimiento] Pedido {} esta en estado {} (clave={} particion={} offset={})",
                estado.pedidoId(), estado.estado(), registro.key(), registro.partition(), registro.offset());
    }
}