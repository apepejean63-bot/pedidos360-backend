package cl.duocuc.pedidos360.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/00000000-0000-0000-0000-000000000000/v2.0",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.microsoftonline.com/00000000-0000-0000-0000-000000000000/discovery/v2.0/keys",
        "azure.client-id=test-client-id",
        "pedidos.service.url=http://localhost:8081"
})
class PedidoProxySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinToken_deberiaRetornar401() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isUnauthorized());
    }
}
