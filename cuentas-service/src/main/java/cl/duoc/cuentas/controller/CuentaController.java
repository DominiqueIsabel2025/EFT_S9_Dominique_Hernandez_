package cl.duoc.cuentas.controller;

import cl.duoc.cuentas.model.Cuenta;
import cl.duoc.cuentas.repository.CuentaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/legacy/cuentas")
public class CuentaController {

    private final CuentaRepository cuentaRepository;

    public CuentaController(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    // ------------------------- Consultas -------------------------

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<Cuenta> obtenerCuenta(@PathVariable String numeroCuenta) {
        return cuentaRepository.findById(numeroCuenta)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Cuenta>> obtenerCuentasPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cuentaRepository.findByClienteId(clienteId));
    }

    // ------------------------- Apertura / cierre / mantenimiento -------------------------

    public record AperturaRequest(Long clienteId, String tipoCuenta, BigDecimal depositoInicial,
                                  BigDecimal limiteRetiroDiarioCajero) {}

    @PostMapping
    public ResponseEntity<?> abrirCuenta(@RequestBody AperturaRequest req) {
        if (req == null || req.clienteId() == null || req.tipoCuenta() == null) {
            return ResponseEntity.badRequest().body("clienteId y tipoCuenta son obligatorios");
        }
        BigDecimal inicial = req.depositoInicial() == null ? BigDecimal.ZERO : req.depositoInicial();
        BigDecimal limite = req.limiteRetiroDiarioCajero() == null ? new BigDecimal("200000") : req.limiteRetiroDiarioCajero();
        String numero = String.format("CTA-%04d", cuentaRepository.count() + 1);
        Cuenta nueva = new Cuenta(numero, req.clienteId(), req.tipoCuenta().toUpperCase(),
                inicial, inicial, "ACTIVA", limite);
        return ResponseEntity.status(201).body(cuentaRepository.save(nueva));
    }

    /** Cambia el estado: ACTIVA, BLOQUEADA o CERRADA (para cerrar el saldo debe ser cero). */
    @PutMapping("/{numeroCuenta}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable String numeroCuenta, @RequestBody Map<String, String> body) {
        String estado = body.get("estado") == null ? "" : body.get("estado").toUpperCase();
        if (!List.of("ACTIVA", "BLOQUEADA", "CERRADA").contains(estado)) {
            return ResponseEntity.badRequest().body("estado debe ser ACTIVA, BLOQUEADA o CERRADA");
        }
        return cuentaRepository.findById(numeroCuenta).map(c -> {
            if ("CERRADA".equals(estado) && c.getSaldoDisponible().compareTo(BigDecimal.ZERO) != 0) {
                return ResponseEntity.status(409).body((Object) "No se puede cerrar una cuenta con saldo distinto de cero");
            }
            c.setEstado(estado);
            return ResponseEntity.ok((Object) cuentaRepository.save(c));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ------------------------- Movimiento de saldos (uso interno de Pagos) -------------------------

    public record MontoRequest(BigDecimal monto) {}

    @PostMapping("/{numeroCuenta}/debito")
    @Transactional
    public ResponseEntity<?> debitar(@PathVariable String numeroCuenta, @RequestBody MontoRequest req) {
        if (req == null || req.monto() == null || req.monto().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("El monto debe ser mayor que cero");
        }
        return cuentaRepository.findById(numeroCuenta).map(c -> {
            if (!"ACTIVA".equals(c.getEstado())) {
                return ResponseEntity.status(409).body((Object) "La cuenta no se encuentra activa");
            }
            if (c.getSaldoDisponible().compareTo(req.monto()) < 0) {
                return ResponseEntity.status(422).body((Object) "Fondos insuficientes");
            }
            c.setSaldoDisponible(c.getSaldoDisponible().subtract(req.monto()));
            c.setSaldoContable(c.getSaldoContable().subtract(req.monto()));
            return ResponseEntity.ok((Object) cuentaRepository.save(c));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{numeroCuenta}/credito")
    @Transactional
    public ResponseEntity<?> acreditar(@PathVariable String numeroCuenta, @RequestBody MontoRequest req) {
        if (req == null || req.monto() == null || req.monto().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("El monto debe ser mayor que cero");
        }
        return cuentaRepository.findById(numeroCuenta).map(c -> {
            if ("CERRADA".equals(c.getEstado())) {
                return ResponseEntity.status(409).body((Object) "La cuenta esta cerrada");
            }
            c.setSaldoDisponible(c.getSaldoDisponible().add(req.monto()));
            c.setSaldoContable(c.getSaldoContable().add(req.monto()));
            return ResponseEntity.ok((Object) cuentaRepository.save(c));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
