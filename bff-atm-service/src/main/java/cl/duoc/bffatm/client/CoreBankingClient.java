package cl.duoc.bffatm.client;

import cl.duoc.bffatm.dto.CuentaLegacyDTO;
import cl.duoc.bffatm.resilience.FallbackSupport;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cliente hacia core-banking-service.
 *  - Lecturas (saldo): Retry + CircuitBreaker + Bulkhead.
 *  - Escritura (retiro): SIN Retry (no es idempotente: reintentar podria descontar dos veces),
 *    solo CircuitBreaker + Bulkhead.
 */
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

    @CircuitBreaker(name = "coreBankingWrite", fallbackMethod = "retiroFallback")
    @Bulkhead(name = "coreBankingWrite")
    public CuentaLegacyDTO retirar(String numeroCuenta, BigDecimal monto) {
        return restClient.post()
                .uri("/api/legacy/cuentas/{numeroCuenta}/retiro", numeroCuenta)
                .body(Map.of("monto", monto))
                .retrieve()
                .body(CuentaLegacyDTO.class);
    }

    @SuppressWarnings("unused")
    private CuentaLegacyDTO cuentaFallback(String numeroCuenta, Throwable t) {
        throw FallbackSupport.handle("obtenerCuenta", t);
    }

    @SuppressWarnings("unused")
    private CuentaLegacyDTO retiroFallback(String numeroCuenta, BigDecimal monto, Throwable t) {
        throw FallbackSupport.handle("retirar", t);
    }
}
