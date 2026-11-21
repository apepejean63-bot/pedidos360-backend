package cl.duocuc.pedidos360.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duocuc.pedidos360.notificaciones.event.PedidoEstadoDTO;
import cl.duocuc.pedidos360.notificaciones.event.PedidoEventoDTO;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/** Comprueba que el JSON que publica pedidos-service se convierte a los DTO del consumidor. */
class DeserializacionDtoTest {

    @Test
    void conviertePedidoEventoDesdeJson() {
        String json = "{\"eventId\":\"10bdec61\",\"tipo\":\"PEDIDO_CREADO\",\"pedidoId\":1,"
                + "\"cliente\":\"Juan Perez\",\"email\":\"juan@example.com\",\"estado\":\"PENDIENTE\","
                + "\"total\":1000000,\"timestamp\":\"2026-11-21T16:51:49.035247800Z\",\"version\":1}";

        try (JsonDeserializer<PedidoEventoDTO> deserializador = new JsonDeserializer<>(PedidoEventoDTO.class, false)) {
            deserializador.addTrustedPackages("*");
            PedidoEventoDTO evento = deserializador.deserialize("pedidos.eventos", json.getBytes(StandardCharsets.UTF_8));

            assertThat(evento.pedidoId()).isEqualTo(1L);
            assertThat(evento.cliente()).isEqualTo("Juan Perez");
            assertThat(evento.total()).isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(evento.version()).isEqualTo(1);
        }
    }

    @Test
    void conviertePedidoEstadoDesdeJson() {
        String json = "{\"pedidoId\":1,\"estado\":\"PENDIENTE\",\"timestamp\":\"2026-11-21T16:51:49Z\"}";

        try (JsonDeserializer<PedidoEstadoDTO> deserializador = new JsonDeserializer<>(PedidoEstadoDTO.class, false)) {
            deserializador.addTrustedPackages("*");
            PedidoEstadoDTO estado = deserializador.deserialize("pedidos.estado", json.getBytes(StandardCharsets.UTF_8));

            assertThat(estado.pedidoId()).isEqualTo(1L);
            assertThat(estado.estado()).isEqualTo("PENDIENTE");
        }
    }
}