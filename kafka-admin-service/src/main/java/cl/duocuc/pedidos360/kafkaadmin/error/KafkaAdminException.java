package cl.duocuc.pedidos360.kafkaadmin.error;

import org.springframework.http.HttpStatus;

/** Error de negocio con el codigo HTTP que corresponde (404, 409, 400, 503...). */
public class KafkaAdminException extends RuntimeException {

    private final HttpStatus status;

    public KafkaAdminException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}