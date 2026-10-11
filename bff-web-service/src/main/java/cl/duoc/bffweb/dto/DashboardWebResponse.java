package cl.duoc.bffweb.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta enriquecida y completa para el canal Web: incluye datos del
 * cliente, todas sus cuentas con el detalle completo de saldos y el
 * historial completo de transacciones. Pensada para dashboards complejos
 * donde el ancho de banda no es una restriccion critica.
 */
public record DashboardWebResponse(
        Long clienteId,
        String nombreCompleto,
        String rut,
        String email,
        String telefono,
        String segmento,
        List<CuentaDetalleWeb> cuentas
) {
    public record CuentaDetalleWeb(
            String numeroCuenta,
            String tipoCuenta,
            String estado,
            BigDecimal saldoDisponible,
            BigDecimal saldoContable,
            BigDecimal limiteRetiroDiarioCajero,
            List<TransaccionLegacyDTO> historialCompleto
    ) {}
}
