package cl.duoc.bffweb.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.web.client.HttpClientErrorException;

/** Traduce el error tecnico original en una excepcion de negocio controlada. */
public final class FallbackSupport {

    private FallbackSupport() {}

    public static RuntimeException handle(String operacion, Throwable t) {
        // Errores 4xx del core (404, 409, 422...) son respuestas de negocio validas: se propagan tal cual.
        if (t instanceof HttpClientErrorException clientError) {
            return clientError;
        }
        String motivo;
        if (t instanceof CallNotPermittedException) {
            motivo = "circuit breaker ABIERTO";
        } else if (t instanceof BulkheadFullException) {
            motivo = "bulkhead saturado";
        } else {
            motivo = t.getClass().getSimpleName();
        }
        return new ServicioNoDisponibleException(
                "Core Banking no disponible temporalmente (" + operacion + "): " + motivo);
    }
}
