package cl.duocuc.pedidos360.notificaciones.event;

import java.math.BigDecimal;

/** Evento del topic pedidos.eventos (mismo contrato JSON que publica pedidos-service). */
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