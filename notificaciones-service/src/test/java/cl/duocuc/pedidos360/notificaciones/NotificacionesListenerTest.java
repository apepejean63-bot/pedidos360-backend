package cl.duocuc.pedidos360.notificaciones;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duocuc.pedidos360.notificaciones.error.EventoInvalidoException;
import cl.duocuc.pedidos360.notificaciones.error.ServicioCorreoNoDisponibleException;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import cl.duocuc.pedidos360.notificaciones.listener.NotificacionesListener;
import java.math.BigDecimal;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class NotificacionesListenerTest {

    private final NotificacionesListener listener = new NotificacionesListener();

    private ConsumerRecord<String, PedidoEventoDTO> registro(String email) {
        PedidoEventoDTO dto = new PedidoEventoDTO("evt-1", "PEDIDO_CREADO", 1L, "Cliente", email, "PENDIENTE",
                new BigDecimal("1000"), "2026-11-21T16:51:49Z", 1);
        return new ConsumerRecord<>("pedidos.eventos", 0, 0L, "1", dto);
    }

    @Test
    void procesaEventoValido() {
        assertThatCode(() -> listener.alRecibirEvento(registro("juan@example.com")))
                .doesNotThrowAnyException();
    }

    @Test
    void lanzaErrorReintentableSiElProveedorDeCorreoFalla() {
        assertThatThrownBy(() -> listener.alRecibirEvento(registro("cliente@falla.test")))
                .isInstanceOf(ServicioCorreoNoDisponibleException.class);
    }

    @Test
    void rechazaEventoInvalidoSinReintentar() {
        assertThatThrownBy(() -> listener.alRecibirEvento(registro(null)))
                .isInstanceOf(EventoInvalidoException.class);
    }
}