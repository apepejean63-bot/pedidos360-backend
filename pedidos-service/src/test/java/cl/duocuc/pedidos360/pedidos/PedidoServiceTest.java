package cl.duocuc.pedidos360.pedidos;

import cl.duocuc.pedidos360.pedidos.dto.DetalleRequest;
import cl.duocuc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duocuc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duocuc.pedidos360.pedidos.entity.Pedido;
import cl.duocuc.pedidos360.pedidos.repository.PedidoRepository;
import cl.duocuc.pedidos360.pedidos.service.PedidoNotFoundException;
import cl.duocuc.pedidos360.pedidos.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidoServiceTest {

    private PedidoRepository pedidoRepository;
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        pedidoService = new PedidoService(pedidoRepository);
    }

    @Test
    void crear_deberiaGuardarPedidoConEstadoPendiente() {
        PedidoRequest request = new PedidoRequest();
        request.setCliente("Juan Perez");
        request.setEmail("juan@example.com");

        DetalleRequest detalle = new DetalleRequest();
        detalle.setProducto("Notebook");
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("500000"));
        request.setDetalles(List.of(detalle));

        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido resultado = pedidoService.crear(request);

        assertEquals("Juan Perez", resultado.getCliente());
        assertEquals(EstadoPedido.PENDIENTE, resultado.getEstado());
        assertEquals(1, resultado.getDetalles().size());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void buscarPorId_deberiaLanzarExcepcion_siNoExiste() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PedidoNotFoundException.class, () -> pedidoService.buscarPorId(99L));
    }

    @Test
    void cambiarEstado_deberiaActualizarElEstado() {
        Pedido pedidoExistente = new Pedido();
        pedidoExistente.setId(1L);
        pedidoExistente.setEstado(EstadoPedido.PENDIENTE);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoExistente));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido actualizado = pedidoService.cambiarEstado(1L, EstadoPedido.ENVIADO);

        assertEquals(EstadoPedido.ENVIADO, actualizado.getEstado());
    }
}
