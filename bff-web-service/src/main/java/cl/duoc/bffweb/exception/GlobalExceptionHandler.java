package cl.duoc.bffweb.exception;

import cl.duoc.bffweb.resilience.ServicioNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> servicioNoDisponible(ServicioNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(cuerpo("SERVICIO_NO_DISPONIBLE", ex.getMessage()));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> errorDelCore(HttpClientErrorException ex) {
        String detalle = ex.getResponseBodyAsString();
        if (detalle == null || detalle.isBlank()) {
            detalle = ex.getStatusText();
        }
        return ResponseEntity.status(ex.getStatusCode()).body(cuerpo("ERROR_CORE_BANKING", detalle));
    }

    private Map<String, Object> cuerpo(String error, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("mensaje", mensaje);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
