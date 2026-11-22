package cl.duocuc.pedidos360.kafkaadmin.service;

import cl.duocuc.pedidos360.kafkaadmin.dto.Modelos.LagParticion;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/** Calcula el lag: cuantos mensajes faltan por leer = offset final - offset confirmado. */
public final class CalculadorLag {

    private CalculadorLag() {
    }

    public static List<LagParticion> calcular(
            Map<TopicPartition, OffsetAndMetadata> confirmados,
            Map<TopicPartition, Long> finales) {
        return finales.entrySet().stream()
                .map(entrada -> {
                    TopicPartition particion = entrada.getKey();
                    long fin = entrada.getValue();
                    OffsetAndMetadata confirmado = confirmados.get(particion);
                    // -1 significa que el grupo todavia no confirmo nada en esa particion
                    long actual = confirmado == null ? -1 : confirmado.offset();
                    long lag = Math.max(0, fin - Math.max(actual, 0));
                    return new LagParticion(particion.topic(), particion.partition(), actual, fin, lag);
                })
                .sorted(Comparator.comparing(LagParticion::topic).thenComparingInt(LagParticion::particion))
                .toList();
    }
}