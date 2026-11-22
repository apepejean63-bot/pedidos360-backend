package cl.duocuc.pedidos360.kafkaadmin;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.LagParticion;
import cl.duocuc.pedidos360.kafkaadmin.service.CalculadorLag;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class CalculadorLagTest {

    private final TopicPartition p0 = new TopicPartition("pedidos.eventos", 0);
    private final TopicPartition p1 = new TopicPartition("pedidos.eventos", 1);
    private final TopicPartition p2 = new TopicPartition("pedidos.eventos", 2);

    @Test
    void calculaElLagPorParticion() {
        Map<TopicPartition, OffsetAndMetadata> confirmados = Map.of(
                p0, new OffsetAndMetadata(1),
                p1, new OffsetAndMetadata(5));
        Map<TopicPartition, Long> finales = Map.of(p0, 1L, p1, 8L);

        List<LagParticion> lag = CalculadorLag.calcular(confirmados, finales);

        assertThat(lag).hasSize(2);
        assertThat(lag.get(0).particion()).isEqualTo(0);
        assertThat(lag.get(0).lag()).isEqualTo(0);
        assertThat(lag.get(1).particion()).isEqualTo(1);
        assertThat(lag.get(1).lag()).isEqualTo(3);
    }

    @Test
    void siElGrupoNoConfirmoNadaElLagEsTodoElTopic() {
        Map<TopicPartition, OffsetAndMetadata> confirmados = Map.of();
        Map<TopicPartition, Long> finales = Map.of(p2, 4L);

        List<LagParticion> lag = CalculadorLag.calcular(confirmados, finales);

        assertThat(lag).hasSize(1);
        assertThat(lag.get(0).offsetActual()).isEqualTo(-1);
        assertThat(lag.get(0).lag()).isEqualTo(4);
    }

    @Test
    void ordenaPorTopicYParticion() {
        Map<TopicPartition, Long> finales = Map.of(p2, 1L, p0, 1L, p1, 1L);

        List<LagParticion> lag = CalculadorLag.calcular(Map.of(), finales);

        assertThat(lag).extracting(LagParticion::particion).containsExactly(0, 1, 2);
    }
}