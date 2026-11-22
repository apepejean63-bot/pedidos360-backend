package cl.duocuc.pedidos360.notificaciones;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duocuc.pedidos360.notificaciones.error.EventoInvalidoException;
import cl.duocuc.pedidos360.notificaciones.error.ValidadorEventos;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEstadoDTO;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import java.math.BigDecimal;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class ValidadorEventosTest {

    private ConsumerRecord<String, PedidoEventoDTO> evento(Long pedidoId, String email, String total, String estado) {
        PedidoEventoDTO dto = new PedidoEventoDTO("evt-1", "PEDIDO_CREADO", pedidoId, "Cliente", email, estado,
                total == null ? null : new BigDecimal(total), "2026-11-21T16:51:49Z", 1);
        return new ConsumerRecord<>("pedidos.eventos", 1, 7L, "1", dto);
    }

    @Test
    void aceptaEventoValido() {
        assertThatCode(() -> ValidadorEventos.validarEvento(evento(1L, "juan@example.com", "1000", "PENDIENTE")))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaEventoSinEmail() {
        assertThatThrownBy(() -> ValidadorEventos.validarEvento(evento(1L, null, "1000", "PENDIENTE")))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("email")
                .hasMessageContaining("pedidos.eventos-1")
                .hasMessageContaining("offset 7");
    }

    @Test
    void rechazaEventoConTotalNegativo() {
        assertThatThrownBy(() -> ValidadorEventos.validarEvento(evento(1L, "juan@example.com", "-5", "PENDIENTE")))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("total");
    }

    @Test
    void rechazaMensajeSinContenido() {
        ConsumerRecord<String, PedidoEventoDTO> vacio = new ConsumerRecord<>("pedidos.eventos", 0, 0L, "1", null);
        assertThatThrownBy(() -> ValidadorEventos.validarEvento(vacio))
                .isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    void rechazaEstadoSinEstado() {
        PedidoEstadoDTO dto = new PedidoEstadoDTO(1L, " ", "2026-11-21T16:51:49Z");
        ConsumerRecord<String, PedidoEstadoDTO> registro = new ConsumerRecord<>("pedidos.estado", 0, 0L, "1", dto);
        assertThatThrownBy(() -> ValidadorEventos.validarEstado(registro))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("estado");
    }
}