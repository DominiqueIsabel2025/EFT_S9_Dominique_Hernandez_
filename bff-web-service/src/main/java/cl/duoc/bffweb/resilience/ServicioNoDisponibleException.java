package cl.duoc.bffweb.resilience;

/** Se lanza cuando el core-banking-service no esta disponible (fallback de Resilience4j). */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
