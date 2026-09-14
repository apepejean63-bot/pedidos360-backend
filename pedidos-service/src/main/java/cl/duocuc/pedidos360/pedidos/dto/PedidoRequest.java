package cl.duocuc.pedidos360.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class PedidoRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String cliente;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotEmpty(message = "El pedido debe tener al menos un detalle")
    @Valid
    private List<DetalleRequest> detalles;

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<DetalleRequest> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleRequest> detalles) {
        this.detalles = detalles;
    }
}
