package cl.duoc.pagos.client;

import cl.duoc.pagos.dto.CuentaDTO;
import cl.duoc.pagos.resilience.FallbackSupport;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cliente hacia cuentas-service (descubierto por Eureka).
 * Consulta: Retry + CircuitBreaker + Bulkhead. Debito/credito: sin Retry (no son idempotentes).
 */
@Component
public class CuentasClient {

    private final RestClient restClient;

    public CuentasClient(RestClient cuentasRestClient) {
        this.restClient = cuentasRestClient;
    }

    @Retry(name = "cuentasRead", fallbackMethod = "consultaFallback")
    @CircuitBreaker(name = "cuentasRead")
    @Bulkhead(name = "cuentasRead")
    public CuentaDTO obtener(String numeroCuenta) {
        return restClient.get()
                .uri("/api/legacy/cuentas/{n}", numeroCuenta)
                .retrieve()
                .body(CuentaDTO.class);
    }

    @CircuitBreaker(name = "cuentasWrite", fallbackMethod = "debitoFallback")
    @Bulkhead(name = "cuentasWrite")
    public CuentaDTO debitar(String numeroCuenta, BigDecimal monto) {
        return restClient.post()
                .uri("/api/legacy/cuentas/{n}/debito", numeroCuenta)
                .body(Map.of("monto", monto))
                .retrieve()
                .body(CuentaDTO.class);
    }

    @CircuitBreaker(name = "cuentasWrite", fallbackMethod = "creditoFallback")
    @Bulkhead(name = "cuentasWrite")
    public CuentaDTO acreditar(String numeroCuenta, BigDecimal monto) {
        return restClient.post()
                .uri("/api/legacy/cuentas/{n}/credito", numeroCuenta)
                .body(Map.of("monto", monto))
                .retrieve()
                .body(CuentaDTO.class);
    }

    @SuppressWarnings("unused")
    private CuentaDTO consultaFallback(String numeroCuenta, Throwable t) {
        throw FallbackSupport.handle("obtener", t);
    }

    @SuppressWarnings("unused")
    private CuentaDTO debitoFallback(String numeroCuenta, BigDecimal monto, Throwable t) {
        throw FallbackSupport.handle("debitar", t);
    }

    @SuppressWarnings("unused")
    private CuentaDTO creditoFallback(String numeroCuenta, BigDecimal monto, Throwable t) {
        throw FallbackSupport.handle("acreditar", t);
    }
}
