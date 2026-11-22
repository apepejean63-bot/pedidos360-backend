package cl.duocuc.pedidos360.notificaciones.error;

import cl.duocuc.pedidos360.notificaciones.event.PedidoEstadoDTO;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/** Valida el contenido de los mensajes y deja en el error el topico, la particion y el offset. */
public final class ValidadorEventos {

    private ValidadorEventos() {
    }

    public static void validarEvento(ConsumerRecord<String, PedidoEventoDTO> registro) {
        PedidoEventoDTO evento = registro.value();
        if (evento == null) {
            throw invalido(registro, "el mensaje no tiene contenido");
        }
        if (evento.pedidoId() == null || evento.pedidoId() <= 0) {
            throw invalido(registro, "pedidoId ausente o invalido");
        }
        if (evento.email() == null || !evento.email().contains("@")) {
            throw invalido(registro, "email ausente o invalido");
        }
        if (evento.total() == null || evento.total().signum() < 0) {
            throw invalido(registro, "total ausente o negativo");
        }
        if (evento.estado() == null || evento.estado().isBlank()) {
            throw invalido(registro, "estado ausente");
        }
    }

    public static void validarEstado(ConsumerRecord<String, PedidoEstadoDTO> registro) {
        PedidoEstadoDTO estado = registro.value();
        if (estado == null) {
            throw invalido(registro, "el mensaje no tiene contenido");
        }
        if (estado.pedidoId() == null || estado.pedidoId() <= 0) {
            throw invalido(registro, "pedidoId ausente o invalido");
        }
        if (estado.estado() == null || estado.estado().isBlank()) {
            throw invalido(registro, "estado ausente");
        }
    }

    private static EventoInvalidoException invalido(ConsumerRecord<?, ?> registro, String motivo) {
        return new EventoInvalidoException(String.format(
                "Mensaje invalido en %s-%d offset %d (clave=%s): %s",
                registro.topic(), registro.partition(), registro.offset(), registro.key(), motivo));
    }
}