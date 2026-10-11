package cl.duoc.pagos.controller;

import cl.duoc.pagos.model.Transaccion;
import cl.duoc.pagos.repository.TransaccionRepository;
import cl.duoc.pagos.service.PagosService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class PagosController {

    private final PagosService pagosService;
    private final TransaccionRepository transaccionRepository;
    private final BigDecimal montoMaximoTransferencia;

    public PagosController(PagosService pagosService, TransaccionRepository transaccionRepository,
                           // viene del Config Server (pagos-service.yml); 5.000.000 si no esta disponible
                           @Value("${pagos.monto-maximo-transferencia:5000000}") BigDecimal montoMaximoTransferencia) {
        this.pagosService = pagosService;
        this.transaccionRepository = transaccionRepository;
        this.montoMaximoTransferencia = montoMaximoTransferencia;
    }

    public record RetiroRequest(BigDecimal monto) {}
    public record OperacionRequest(String numeroCuenta, BigDecimal monto, String canal, String descripcion) {}
    public record TransferenciaRequest(String cuentaOrigen, String cuentaDestino, BigDecimal monto, String canal) {}

    private static boolean montoInvalido(BigDecimal m) {
        return m == null || m.compareTo(BigDecimal.ZERO) <= 0;
    }

    // Contrato original que usa el BFF de cajeros (el gateway enruta esta ruta a este servicio)
    @PostMapping("/api/legacy/cuentas/{numeroCuenta}/retiro")
    public ResponseEntity<?> retirar(@PathVariable String numeroCuenta, @RequestBody RetiroRequest req) {
        if (req == null || montoInvalido(req.monto())) {
            return ResponseEntity.badRequest().body("El monto debe ser mayor que cero");
        }
        return ResponseEntity.ok(pagosService.egreso(numeroCuenta, req.monto(), "RETIRO", "CAJERO", "Retiro cajero"));
    }

    @PostMapping("/api/pagos/deposito")
    public ResponseEntity<?> depositar(@RequestBody OperacionRequest req) {
        if (req == null || req.numeroCuenta() == null || montoInvalido(req.monto())) {
            return ResponseEntity.badRequest().body("numeroCuenta y un monto mayor que cero son obligatorios");
        }
        return ResponseEntity.ok(pagosService.deposito(req.numeroCuenta(), req.monto(),
                req.canal() == null ? "WEB" : req.canal(),
                req.descripcion() == null ? "Deposito" : req.descripcion()));
    }

    @PostMapping("/api/pagos/pago")
    public ResponseEntity<?> pagar(@RequestBody OperacionRequest req) {
        if (req == null || req.numeroCuenta() == null || montoInvalido(req.monto())) {
            return ResponseEntity.badRequest().body("numeroCuenta y un monto mayor que cero son obligatorios");
        }
        return ResponseEntity.ok(pagosService.egreso(req.numeroCuenta(), req.monto(), "PAGO",
                req.canal() == null ? "WEB" : req.canal(),
                req.descripcion() == null ? "Pago de servicio" : req.descripcion()));
    }

    @PostMapping("/api/pagos/transferencia")
    public ResponseEntity<?> transferir(@RequestBody TransferenciaRequest req) {
        if (req == null || req.cuentaOrigen() == null || req.cuentaDestino() == null || montoInvalido(req.monto())) {
            return ResponseEntity.badRequest().body("cuentaOrigen, cuentaDestino y monto (> 0) son obligatorios");
        }
        if (req.cuentaOrigen().equals(req.cuentaDestino())) {
            return ResponseEntity.badRequest().body("La cuenta origen y destino deben ser distintas");
        }
        if (req.monto().compareTo(montoMaximoTransferencia) > 0) {
            return ResponseEntity.status(422).body("El monto supera el maximo por transferencia: " + montoMaximoTransferencia);
        }
        return ResponseEntity.ok(pagosService.transferencia(req.cuentaOrigen(), req.cuentaDestino(), req.monto(),
                req.canal() == null ? "WEB" : req.canal()));
    }

    @GetMapping("/api/legacy/transacciones/cuenta/{numeroCuenta}")
    public ResponseEntity<List<Transaccion>> obtenerPorCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(transaccionRepository.findByNumeroCuentaOrderByFechaDesc(numeroCuenta));
    }
}
