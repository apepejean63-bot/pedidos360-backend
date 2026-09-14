package cl.duocuc.pedidos360.pedidos.service;

public class PedidoNotFoundException extends RuntimeException {
    public PedidoNotFoundException(Long id) {
        super("No se encontró el pedido con id " + id);
    }
}
