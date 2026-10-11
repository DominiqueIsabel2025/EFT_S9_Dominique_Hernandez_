package cl.duoc.notification.dto;

import java.math.BigDecimal;

/** Mismo contrato que el evento publicado por core-banking-service. */
public record TransaccionEvent(
        String eventId,
        String numeroCuenta,
        String tipoMovimiento,
        BigDecimal monto,
        BigDecimal saldoDisponible,
        String canalOrigen,
        String fecha
) {}
