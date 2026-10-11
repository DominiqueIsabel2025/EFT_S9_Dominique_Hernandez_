package cl.duoc.notification.listener;

import cl.duoc.notification.controller.NotificationStore;
import cl.duoc.notification.dto.Notificacion;
import cl.duoc.notification.dto.TransaccionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Consumidor asincrono: reacciona a los eventos publicados por core-banking-service.
 * Si el monto supera el umbral configurado genera una ALERTA (simulacion de aviso al cliente / antifraude).
 */
@Component
public class TransaccionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransaccionEventListener.class);

    private final NotificationStore store;
    private final BigDecimal montoAlto;

    public TransaccionEventListener(NotificationStore store,
                                    @Value("${app.alerta.monto-alto}") BigDecimal montoAlto) {
        this.store = store;
        this.montoAlto = montoAlto;
    }

    @KafkaListener(topics = "${app.kafka.topic-transacciones}", groupId = "notification-group")
    public void onTransaccion(TransaccionEvent event) {
        if (event == null) {
            log.warn("[KAFKA] Evento nulo ignorado");
            return;
        }
        boolean alta = event.monto() != null && event.monto().compareTo(montoAlto) >= 0;
        String nivel = alta ? "ALERTA_MONTO_ALTO" : "INFO";
        String mensaje = String.format("%s de $%s en cuenta %s via %s. Saldo disponible: $%s",
                event.tipoMovimiento(), event.monto(), event.numeroCuenta(),
                event.canalOrigen(), event.saldoDisponible());

        Notificacion notificacion = new Notificacion(event.eventId(), event.numeroCuenta(),
                event.tipoMovimiento(), event.monto(), event.canalOrigen(), nivel, mensaje,
                Instant.now().toString());
        store.agregar(notificacion);

        log.info("[NOTIFICACION][{}] {}", nivel, mensaje);
    }
}
