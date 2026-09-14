package cl.duocuc.pedidos360.pedidos.controller;

import cl.duocuc.pedidos360.pedidos.dto.CambiarEstadoRequest;
import cl.duocuc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duocuc.pedidos360.pedidos.entity.Pedido;
import cl.duocuc.pedidos360.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Cualquier usuario autenticado (token JWT valido) puede listar y ver pedidos
    @GetMapping
    public ResponseEntity<List<Pedido>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    // Crear pedido: cualquier usuario autenticado
    @PostMapping
    public ResponseEntity<Pedido> crear(@Valid @RequestBody PedidoRequest request) {
        Pedido creado = pedidoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Cambiar estado: solo usuarios con rol Admin (viene del claim "roles" del JWT de Azure AD)
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Pedido> cambiarEstado(@PathVariable Long id,
                                                 @Valid @RequestBody CambiarEstadoRequest request) {
        Pedido actualizado = pedidoService.cambiarEstado(id, request.getEstado());
        return ResponseEntity.ok(actualizado);
    }

    // Eliminar: solo Admin
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
