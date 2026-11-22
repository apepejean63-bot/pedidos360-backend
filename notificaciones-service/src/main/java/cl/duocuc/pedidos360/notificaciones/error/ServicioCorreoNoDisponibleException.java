package cl.duocuc.pedidos360.notificaciones.error;

/** Falla temporal del proveedor de correo. Se reintenta y, si sigue fallando, va a la DLT. */
public class ServicioCorreoNoDisponibleException extends RuntimeException {

    public ServicioCorreoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}