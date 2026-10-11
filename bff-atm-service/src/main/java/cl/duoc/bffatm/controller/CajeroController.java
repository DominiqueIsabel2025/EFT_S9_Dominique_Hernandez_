package cl.duoc.bffatm.controller;

import cl.duoc.bffatm.client.CoreBankingClient;
import cl.duoc.bffatm.dto.CuentaLegacyDTO;
import cl.duoc.bffatm.dto.RetiroCajeroRequest;
import cl.duoc.bffatm.dto.RetiroCajeroResponse;
import cl.duoc.bffatm.dto.SaldoCajeroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;

/**
 * BFF Cajero Automatico: expone solo las 2 operaciones criticas que un cajero necesita
 * (consulta de saldo y retiro) con el minimo de datos posible y la regla propia del canal
 * (limite diario de retiro).
 * Seguridad: OAuth2 - requiere un JWT con scope "atm.operate" (reemplaza al antiguo X-ATM-KEY).
 */
@RestController
@RequestMapping("/atm/cuentas")
public class CajeroController {

    private final CoreBankingClient coreBankingClient;

    public CajeroController(CoreBankingClient coreBankingClient) {
        this.coreBankingClient = coreBankingClient;
    }

    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<SaldoCajeroResponse> consultarSaldo(@PathVariable String numeroCuenta) {
        CuentaLegacyDTO cuenta = coreBankingClient.obtenerCuenta(numeroCuenta);
        return ResponseEntity.ok(new SaldoCajeroResponse(cuenta.numeroCuenta(), cuenta.saldoDisponible(), cuenta.estado()));
    }

    @PostMapping("/{numeroCuenta}/retiro")
    public ResponseEntity<RetiroCajeroResponse> retirar(@PathVariable String numeroCuenta,
                                                        @RequestBody RetiroCajeroRequest request) {
        if (request == null || request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(new RetiroCajeroResponse(numeroCuenta, request == null ? null : request.monto(), null,
                            "El monto debe ser mayor que cero"));
        }

        CuentaLegacyDTO cuenta = coreBankingClient.obtenerCuenta(numeroCuenta);

        // Regla especifica del canal Cajero: no superar el limite diario configurado.
        if (request.monto().compareTo(cuenta.limiteRetiroDiarioCajero()) > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RetiroCajeroResponse(numeroCuenta, request.monto(), cuenta.saldoDisponible(),
                            "Monto excede el limite diario de retiro por cajero ($" + cuenta.limiteRetiroDiarioCajero() + ")"));
        }

        try {
            CuentaLegacyDTO actualizada = coreBankingClient.retirar(numeroCuenta, request.monto());
            return ResponseEntity.ok(new RetiroCajeroResponse(
                    numeroCuenta, request.monto(), actualizada.saldoDisponible(), "Retiro exitoso"));
        } catch (HttpClientErrorException ex) {
            // 409 cuenta inactiva / 422 fondos insuficientes: respuesta de negocio, no es una falla tecnica.
            return ResponseEntity.status(ex.getStatusCode())
                    .body(new RetiroCajeroResponse(numeroCuenta, request.monto(), cuenta.saldoDisponible(),
                            ex.getResponseBodyAsString()));
        }
    }
}
