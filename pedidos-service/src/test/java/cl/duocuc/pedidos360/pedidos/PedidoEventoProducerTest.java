package cl.duocuc.pedidos360.pedidos;

import cl.duocuc.pedidos360.pedidos.entity.DetallePedido;
import cl.duocuc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duocuc.pedidos360.pedidos.entity.Pedido;
import cl.duocuc.pedidos360.pedidos.event.PedidoEstadoDTO;
import cl.duocuc.pedidos360.pedidos.event.PedidoEventoDTO;
import cl.duocuc.pedidos360.pedidos.messaging.PedidoEventoProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PedidoEventoProducerTest {

    private KafkaTemplate<String, Object> kafkaTemplate;
    private PedidoEventoProducer producer;
    private Pedido pedido;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new PedidoEventoProducer(kafkaTemplate);

        pedido = new Pedido();
        pedido.setId(7L);
        pedido.setCliente("Juan Perez");
        pedido.setEmail("juan@example.com");
        pedido.setEstado(EstadoPedido.PENDIENTE);

        DetallePedido detalle = new DetallePedido();
        detalle.setProducto("Notebook");
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("1000"));
        pedido.addDetalle(detalle);
    }

    @Test
    void publicarCreado_deberiaEnviarEventoYEstadoUsandoPedidoIdComoClave() {

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

        producer.publicarCreado(pedido);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("pedidos.eventos"), eq("7"), captor.capture());

        PedidoEventoDTO evento = (PedidoEventoDTO) captor.getValue();
        assertEquals("PEDIDO_CREADO", evento.tipo());
        assertEquals(7L, evento.pedidoId());
        assertEquals("PENDIENTE", evento.estado());
        assertEquals(0, new BigDecimal("2000").compareTo(evento.total()));
        assertNotNull(evento.eventId());

        verify(kafkaTemplate).send(eq("pedidos.estado"), eq("7"), any(PedidoEstadoDTO.class));
    }

    @Test
    void publicar_noDeberiaPropagarElError_siKafkaFalla() {

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka caido"));

        assertDoesNotThrow(() -> producer.publicarCreado(pedido));
    }
}