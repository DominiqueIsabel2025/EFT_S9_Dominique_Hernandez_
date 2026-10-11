package cl.duoc.pagos.controller;

import cl.duoc.pagos.resilience.ServicioNoDisponibleException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Errores de negocio de cuentas-service (404, 409, 422): se devuelven con el mismo codigo y mensaje. */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleNegocio(HttpClientErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleNoDisponible(ServicioNoDisponibleException ex) {
        return ResponseEntity.status(503).body(Map.of("error", "SERVICIO_NO_DISPONIBLE", "mensaje", ex.getMessage()));
    }
}
