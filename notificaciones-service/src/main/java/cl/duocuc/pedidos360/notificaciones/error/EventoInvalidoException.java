package cl.duocuc.pedidos360.notificaciones.error;

/** El mensaje llego bien, pero su contenido no es valido. Reintentar no sirve, va directo a la DLT. */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}