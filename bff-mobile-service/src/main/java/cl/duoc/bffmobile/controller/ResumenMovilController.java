package cl.duoc.bffmobile.controller;

import cl.duoc.bffmobile.client.CoreBankingClient;
import cl.duoc.bffmobile.dto.CuentaLegacyDTO;
import cl.duoc.bffmobile.dto.MovimientoMovilDTO;
import cl.duoc.bffmobile.dto.ResumenMovilResponse;
import cl.duoc.bffmobile.dto.TransaccionLegacyDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * BFF Movil: transforma la respuesta del legacy en un payload minimo,
 * pensado para reducir el consumo de ancho de banda y acelerar la carga
 * en la app movil. Solo entrega el saldo y las ultimas 5 transacciones.
 */
@RestController
@RequestMapping("/mobile/cuentas")
public class ResumenMovilController {

    private final CoreBankingClient coreBankingClient;

    public ResumenMovilController(CoreBankingClient coreBankingClient) {
        this.coreBankingClient = coreBankingClient;
    }

    @GetMapping("/{numeroCuenta}/resumen")
    public ResponseEntity<ResumenMovilResponse> obtenerResumen(@PathVariable String numeroCuenta) {
        // Si la cuenta no existe, el core responde 404 y GlobalExceptionHandler lo traduce.
        CuentaLegacyDTO cuenta = coreBankingClient.obtenerCuenta(numeroCuenta);

        List<TransaccionLegacyDTO> transacciones = coreBankingClient.obtenerTransacciones(numeroCuenta);

        List<MovimientoMovilDTO> ultimosMovimientos = transacciones.stream()
                .sorted(Comparator.comparing(TransaccionLegacyDTO::fecha).reversed())
                .limit(5)
                .map(t -> new MovimientoMovilDTO(t.tipoMovimiento(), t.monto(), t.fecha().toLocalDate(), t.descripcion()))
                .toList();

        ResumenMovilResponse response = new ResumenMovilResponse(
                cuenta.numeroCuenta(), cuenta.tipoCuenta(), cuenta.saldoDisponible(), ultimosMovimientos
        );

        return ResponseEntity.ok(response);
    }
}
