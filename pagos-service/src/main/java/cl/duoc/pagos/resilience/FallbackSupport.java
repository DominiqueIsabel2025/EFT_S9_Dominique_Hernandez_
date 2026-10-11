package cl.duoc.pagos.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.web.client.HttpClientErrorException;

/** Traduce el error tecnico en una excepcion de negocio controlada. */
public final class FallbackSupport {

    private FallbackSupport() {}

    public static RuntimeException handle(String operacion, Throwable t) {
        // Los 4xx (404, 409, 422) son respuestas de negocio validas: se propagan.
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
                "cuentas-service no disponible temporalmente (" + operacion + "): " + motivo);
    }
}
