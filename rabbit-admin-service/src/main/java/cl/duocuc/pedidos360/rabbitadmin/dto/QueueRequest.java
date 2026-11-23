package cl.duocuc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;

public class QueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String name;

    // Opcionales: si se indican, la cola envia los rechazados a ese exchange (DLQ)
    private String deadLetterExchange;
    private String deadLetterRoutingKey;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }
    public String getDeadLetterRoutingKey() { return deadLetterRoutingKey; }
    public void setDeadLetterRoutingKey(String deadLetterRoutingKey) { this.deadLetterRoutingKey = deadLetterRoutingKey; }
}