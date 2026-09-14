package cl.duocuc.pedidos360.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * El BFF NO conoce reglas de negocio de pedidos: solo valida el JWT (ya lo hace
 * el SecurityConfig antes de llegar aqui) y reenvia la peticion al microservicio
 * de dominio, propagando el mismo token de autorizacion.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoProxyController {

    private final RestTemplate restTemplate;

    @Value("${pedidos.service.url}")
    private String pedidosServiceUrl;

    public PedidoProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<String> listarTodos(JwtAuthenticationToken auth) {
        return reenviar(HttpMethod.GET, "", null, auth);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> buscarPorId(@PathVariable Long id, JwtAuthenticationToken auth) {
        return reenviar(HttpMethod.GET, "/" + id, null, auth);
    }

    @PostMapping
    public ResponseEntity<String> crear(@RequestBody String body, JwtAuthenticationToken auth) {
        return reenviar(HttpMethod.POST, "", body, auth);
    }

    // Autorizacion por rol tambien a nivel de BFF (defensa en profundidad,
    // aunque pedidos-service la vuelve a validar igual)
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> cambiarEstado(@PathVariable Long id, @RequestBody String body,
                                                 JwtAuthenticationToken auth) {
        return reenviar(HttpMethod.PATCH, "/" + id + "/estado", body, auth);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> eliminar(@PathVariable Long id, JwtAuthenticationToken auth) {
        return reenviar(HttpMethod.DELETE, "/" + id, null, auth);
    }

    private ResponseEntity<String> reenviar(HttpMethod metodo, String path, String body,
                                             JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt.getTokenValue());
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                pedidosServiceUrl + "/api/v1/pedidos" + path,
                metodo,
                entity,
                String.class
        );
    }
}
