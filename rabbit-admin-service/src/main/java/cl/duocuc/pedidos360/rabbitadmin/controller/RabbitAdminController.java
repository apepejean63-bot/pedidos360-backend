package cl.duocuc.pedidos360.rabbitadmin.controller;

import cl.duocuc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duocuc.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duocuc.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rabbit-admin")
@Validated
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> crearCola(
            @Valid @RequestBody QueueRequest request) {

        rabbitAdminService.crearCola(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "mensaje", "Cola creada correctamente",
                        "queue", request.getName()
                ));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Map<String, String>> eliminarCola(
            @PathVariable
            @NotBlank(message = "El nombre de la cola es obligatorio")
            String name) {

        boolean eliminada = rabbitAdminService.eliminarCola(name);

        if (!eliminada) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "mensaje", "La cola no existe",
                            "queue", name
                    ));
        }

        return ResponseEntity.ok(
                Map.of(
                        "mensaje", "Cola eliminada correctamente",
                        "queue", name
                )
        );
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(
            @Valid @RequestBody ExchangeRequest request) {

        rabbitAdminService.crearExchange(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "mensaje", "Exchange creado correctamente",
                        "exchange", request.getName()
                ));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Map<String, String>> eliminarExchange(
            @PathVariable
            @NotBlank(message = "El nombre del exchange es obligatorio")
            String name) {

        boolean eliminado = rabbitAdminService.eliminarExchange(name);

        if (!eliminado) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "mensaje", "El exchange no existe",
                            "exchange", name
                    ));
        }

        return ResponseEntity.ok(
                Map.of(
                        "mensaje", "Exchange eliminado correctamente",
                        "exchange", name
                )
        );
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(
            @Valid @RequestBody BindingRequest request) {

        rabbitAdminService.crearBinding(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "mensaje", "Binding creado correctamente",
                        "queue", request.getQueueName(),
                        "exchange", request.getExchangeName(),
                        "routingKey", request.getRoutingKey()
                ));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, String>> eliminarBinding(
            @Valid @RequestBody BindingRequest request) {

        rabbitAdminService.eliminarBinding(request);

        return ResponseEntity.ok(
                Map.of(
                        "mensaje", "Binding eliminado correctamente",
                        "queue", request.getQueueName(),
                        "exchange", request.getExchangeName(),
                        "routingKey", request.getRoutingKey()
                )
        );
    }
}