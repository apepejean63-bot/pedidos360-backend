package cl.duocuc.pedidos360.kafkaadmin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duocuc.pedidos360.kafkaadmin.controller.KafkaAdminController;
import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.TopicResumen;
import cl.duocuc.pedidos360.kafkaadmin.error.KafkaAdminException;
import cl.duocuc.pedidos360.kafkaadmin.service.KafkaAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KafkaAdminController.class)
class KafkaAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private KafkaAdminService servicio;

    @Test
    void listaLosTopics() throws Exception {
        when(servicio.listarTopics()).thenReturn(List.of(new TopicResumen("pedidos.eventos", 3, 3)));

        mvc.perform(get("/api/kafka/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("pedidos.eventos"))
                .andExpect(jsonPath("$[0].partitions").value(3));
    }

    @Test
    void creaUnTopicValido() throws Exception {
        mvc.perform(post("/api/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pedidos.demo\",\"partitions\":3,\"replicationFactor\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("pedidos.demo"));

        verify(servicio).crearTopic(any());
    }

    @Test
    void rechazaUnTopicConCeroParticiones() throws Exception {
        mvc.perform(post("/api/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pedidos.demo\",\"partitions\":0,\"replicationFactor\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void respondeNotFoundSiElTopicNoExiste() throws Exception {
        when(servicio.describirTopic("no.existe"))
                .thenThrow(new KafkaAdminException(HttpStatus.NOT_FOUND, "El topic no existe"));

        mvc.perform(get("/api/kafka/topics/no.existe"))
                .andExpect(status().isNotFound());
    }
}