package cl.duoc.notification.dto;

import java.math.BigDecimal;

/** Notificacion generada a partir de un evento de transaccion. */
public record Notificacion(
        String eventId,
        String numeroCuenta,
        String tipo,
        BigDecimal monto,
        String canalOrigen,
        String nivel,       // INFO | ALERTA_MONTO_ALTO
        String mensaje,
        String recibidaEn
) {}
