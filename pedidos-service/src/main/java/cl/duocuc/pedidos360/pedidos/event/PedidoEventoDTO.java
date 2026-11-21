package cl.duocuc.pedidos360.pedidos.event;

import java.math.BigDecimal;

/**
 * Evento de dominio publicado en el topico pedidos.eventos.
 * Se usa como clave el pedidoId para conservar el orden de los eventos de un mismo pedido.
 */
public record PedidoEventoDTO(
        String eventId,
        String tipo,
        Long pedidoId,
        String cliente,
        String email,
        String estado,
        BigDecimal total,
        String timestamp,
        int version) {
}