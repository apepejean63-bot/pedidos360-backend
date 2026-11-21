package cl.duocuc.pedidos360.notificaciones.event;

/** Ultimo estado de un pedido, del topic pedidos.estado (compactado por clave). */
public record PedidoEstadoDTO(
        Long pedidoId,
        String estado,
        String timestamp) {
}