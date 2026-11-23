package cl.duocuc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String name;

    @Pattern(regexp = "direct|topic|fanout", message = "El tipo debe ser direct, topic o fanout")
    private String type = "direct";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type == null ? "direct" : type; }
}