package cl.duocuc.pedidos360.pedidos.service;

import cl.duocuc.pedidos360.pedidos.dto.DetalleRequest;
import cl.duocuc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duocuc.pedidos360.pedidos.entity.DetallePedido;
import cl.duocuc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duocuc.pedidos360.pedidos.entity.Pedido;
import cl.duocuc.pedidos360.pedidos.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNotFoundException(id));
    }

    @Transactional
    public Pedido crear(PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setCliente(request.getCliente());
        pedido.setEmail(request.getEmail());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        for (DetalleRequest detalleReq : request.getDetalles()) {
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(detalleReq.getProducto());
            detalle.setCantidad(detalleReq.getCantidad());
            detalle.setPrecioUnitario(detalleReq.getPrecioUnitario());
            pedido.addDetalle(detalle);
        }

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarPorId(id);
        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new PedidoNotFoundException(id);
        }
        pedidoRepository.deleteById(id);
    }
}
