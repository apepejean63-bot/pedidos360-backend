package cl.duocuc.pedidos360.pedidos.event;

/**
 * Ultimo estado de un pedido, publicado en el topico compactado pedidos.estado.
 */
public record PedidoEstadoDTO(
        Long pedidoId,
        String estado,
        String timestamp) {
}