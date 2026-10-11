package cl.duoc.pagos.resilience;

/** Se lanza cuando cuentas-service no responde (fallback de Resilience4j). */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
