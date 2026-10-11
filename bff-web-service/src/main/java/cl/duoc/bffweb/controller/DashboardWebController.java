package cl.duoc.bffweb.controller;

import cl.duoc.bffweb.client.CoreBankingClient;
import cl.duoc.bffweb.dto.ClienteLegacyDTO;
import cl.duoc.bffweb.dto.CuentaLegacyDTO;
import cl.duoc.bffweb.dto.DashboardWebResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BFF Web: expone datos completos y ricos en detalle, ideales para
 * interfaces complejas (dashboards, graficos, historiales completos).
 * No hay restriccion de tamaño de payload como en Movil o Cajero.
 */
@RestController
@RequestMapping("/web/clientes")
public class DashboardWebController {

    private final CoreBankingClient coreBankingClient;

    public DashboardWebController(CoreBankingClient coreBankingClient) {
        this.coreBankingClient = coreBankingClient;
    }

    @GetMapping("/{clienteId}/dashboard")
    public ResponseEntity<DashboardWebResponse> obtenerDashboard(@PathVariable Long clienteId) {
        // Si el cliente no existe, el core responde 404 y GlobalExceptionHandler lo traduce.
        ClienteLegacyDTO cliente = coreBankingClient.obtenerCliente(clienteId);

        List<CuentaLegacyDTO> cuentas = coreBankingClient.obtenerCuentasPorCliente(clienteId);

        List<DashboardWebResponse.CuentaDetalleWeb> cuentasDetalle = cuentas.stream()
                .map(c -> new DashboardWebResponse.CuentaDetalleWeb(
                        c.numeroCuenta(),
                        c.tipoCuenta(),
                        c.estado(),
                        c.saldoDisponible(),
                        c.saldoContable(),
                        c.limiteRetiroDiarioCajero(),
                        coreBankingClient.obtenerTransacciones(c.numeroCuenta())
                ))
                .toList();

        DashboardWebResponse response = new DashboardWebResponse(
                cliente.id(), cliente.nombreCompleto(), cliente.rut(), cliente.email(),
                cliente.telefono(), cliente.segmento(), cuentasDetalle
        );

        return ResponseEntity.ok(response);
    }
}
