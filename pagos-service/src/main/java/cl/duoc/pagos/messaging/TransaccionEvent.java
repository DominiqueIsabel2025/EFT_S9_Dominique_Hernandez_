package cl.duoc.pagos.messaging;

import java.math.BigDecimal;

/**
 * Evento de dominio publicado en Kafka cada vez que se registra una transaccion.
 * La fecha viaja como String ISO-8601 para mantener el contrato simple entre servicios.
 */
public record TransaccionEvent(
        String eventId,
        String numeroCuenta,
        String tipoMovimiento,
        BigDecimal monto,
        BigDecimal saldoDisponible,
        String canalOrigen,
        String fecha
) {}
