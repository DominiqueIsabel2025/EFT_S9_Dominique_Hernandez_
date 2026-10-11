package cl.duoc.bffweb.client;

import cl.duoc.bffweb.dto.ClienteLegacyDTO;
import cl.duoc.bffweb.dto.CuentaLegacyDTO;
import cl.duoc.bffweb.dto.TransaccionLegacyDTO;
import cl.duoc.bffweb.resilience.FallbackSupport;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cliente HTTP hacia core-banking-service protegido con Resilience4j:
 *  - Retry          : reintenta fallos transitorios (3 intentos, backoff exponencial).
 *  - CircuitBreaker : si falla >=50% de las llamadas, abre el circuito y falla rapido.
 *  - Bulkhead       : limita las llamadas concurrentes para no saturar el hilo del BFF.
 *  - Fallback       : respuesta controlada (503) o degradacion elegante (historial vacio).
 * Los tokens OAuth2 se agregan automaticamente por el interceptor del RestClient.
 */
@Component
public class CoreBankingClient {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingClient.class);

    private final RestClient restClient;

    public CoreBankingClient(RestClient coreBankingRestClient) {
        this.restClient = coreBankingRestClient;
    }

    @Retry(name = "coreBankingRead", fallbackMethod = "clienteFallback")
    @CircuitBreaker(name = "coreBankingRead")
    @Bulkhead(name = "coreBankingRead")
    public ClienteLegacyDTO obtenerCliente(Long clienteId) {
        return restClient.get()
                .uri("/api/legacy/clientes/{id}", clienteId)
                .retrieve()
                .body(ClienteLegacyDTO.class);
    }

    @Retry(name = "coreBankingRead", fallbackMethod = "cuentasFallback")
    @CircuitBreaker(name = "coreBankingRead")
    @Bulkhead(name = "coreBankingRead")
    public List<CuentaLegacyDTO> obtenerCuentasPorCliente(Long clienteId) {
        return restClient.get()
                .uri("/api/legacy/cuentas/cliente/{clienteId}", clienteId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CuentaLegacyDTO>>() {});
    }

    @Retry(name = "coreBankingRead", fallbackMethod = "transaccionesFallback")
    @CircuitBreaker(name = "coreBankingRead")
    @Bulkhead(name = "coreBankingRead")
    public List<TransaccionLegacyDTO> obtenerTransacciones(String numeroCuenta) {
        return restClient.get()
                .uri("/api/legacy/transacciones/cuenta/{numeroCuenta}", numeroCuenta)
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionLegacyDTO>>() {});
    }

    // ---------------------------- Fallbacks ----------------------------

    @SuppressWarnings("unused")
    private ClienteLegacyDTO clienteFallback(Long clienteId, Throwable t) {
        throw FallbackSupport.handle("obtenerCliente", t);
    }

    @SuppressWarnings("unused")
    private List<CuentaLegacyDTO> cuentasFallback(Long clienteId, Throwable t) {
        throw FallbackSupport.handle("obtenerCuentasPorCliente", t);
    }

    /** Degradacion elegante: si falla el historial, el dashboard igual responde (con historial vacio). */
    @SuppressWarnings("unused")
    private List<TransaccionLegacyDTO> transaccionesFallback(String numeroCuenta, Throwable t) {
        if (t instanceof HttpClientErrorException clientError) {
            throw clientError;
        }
        log.warn("[FALLBACK] Historial no disponible para {} ({}). Se devuelve lista vacia.",
                numeroCuenta, t.getClass().getSimpleName());
        return List.of();
    }
}
