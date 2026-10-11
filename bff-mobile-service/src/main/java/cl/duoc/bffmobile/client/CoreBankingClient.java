package cl.duoc.bffmobile.client;

import cl.duoc.bffmobile.dto.CuentaLegacyDTO;
import cl.duoc.bffmobile.dto.TransaccionLegacyDTO;
import cl.duoc.bffmobile.resilience.FallbackSupport;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Cliente hacia core-banking-service con Retry + CircuitBreaker + Bulkhead (Resilience4j) y token OAuth2. */
@Component
public class CoreBankingClient {

    private final RestClient restClient;

    public CoreBankingClient(RestClient coreBankingRestClient) {
        this.restClient = coreBankingRestClient;
    }

    @Retry(name = "coreBankingRead", fallbackMethod = "cuentaFallback")
    @CircuitBreaker(name = "coreBankingRead")
    @Bulkhead(name = "coreBankingRead")
    public CuentaLegacyDTO obtenerCuenta(String numeroCuenta) {
        return restClient.get()
                .uri("/api/legacy/cuentas/{numeroCuenta}", numeroCuenta)
                .retrieve()
                .body(CuentaLegacyDTO.class);
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

    @SuppressWarnings("unused")
    private CuentaLegacyDTO cuentaFallback(String numeroCuenta, Throwable t) {
        throw FallbackSupport.handle("obtenerCuenta", t);
    }

    @SuppressWarnings("unused")
    private List<TransaccionLegacyDTO> transaccionesFallback(String numeroCuenta, Throwable t) {
        throw FallbackSupport.handle("obtenerTransacciones", t);
    }
}
