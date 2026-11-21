package cl.duocuc.pedidos360.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import cl.duocuc.pedidos360.notificaciones.listener.AnaliticaListener;
import java.math.BigDecimal;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class AnaliticaListenerTest {

    private ConsumerRecord<String, PedidoEventoDTO> registro(long pedidoId, String total, long offset) {
        PedidoEventoDTO evento = new PedidoEventoDTO("evt-" + pedidoId, "PEDIDO_CREADO", pedidoId,
                "Cliente " + pedidoId, "c" + pedidoId + "@example.com", "PENDIENTE",
                new BigDecimal(total), "2026-11-21T16:51:49Z", 1);
        return new ConsumerRecord<>("pedidos.eventos", 0, offset, String.valueOf(pedidoId), evento);
    }

    @Test
    void acumulaCantidadDePedidosYMontoTotal() {
        AnaliticaListener listener = new AnaliticaListener();

        listener.alRecibirEvento(registro(1, "100.50", 0));
        listener.alRecibirEvento(registro(2, "200.00", 1));

        assertThat(listener.getTotalPedidos()).isEqualTo(2);
        assertThat(listener.getMontoAcumulado()).isEqualByComparingTo("300.50");
    }
}