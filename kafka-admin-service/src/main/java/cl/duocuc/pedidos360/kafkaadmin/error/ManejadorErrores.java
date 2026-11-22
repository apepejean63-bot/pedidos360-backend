package cl.duocuc.pedidos360.kafkaadmin.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(KafkaAdminException.class)
    public ResponseEntity<ProblemDetail> alFallarKafka(KafkaAdminException error) {
        log.warn("[kafka-admin] {} - {}", error.getStatus().value(), error.getMessage());
        ProblemDetail detalle = ProblemDetail.forStatusAndDetail(error.getStatus(), error.getMessage());
        return ResponseEntity.status(error.getStatus()).body(detalle);
    }
}