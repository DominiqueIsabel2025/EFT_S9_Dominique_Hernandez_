package cl.duoc.pagos.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de transacciones en Kafka de forma asincrona.
 * Un fallo de mensajeria NUNCA debe revertir ni bloquear la operacion bancaria:
 * el error se registra y el flujo principal continua (desacoplamiento).
 */
@Component
public class TransaccionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransaccionEventPublisher.class);

    private final KafkaTemplate<String, TransaccionEvent> kafkaTemplate;
    private final String topic;

    public TransaccionEventPublisher(KafkaTemplate<String, TransaccionEvent> kafkaTemplate,
                                     @Value("${app.kafka.topic-transacciones}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicar(TransaccionEvent event) {
        try {
            // key = numero de cuenta => los eventos de una misma cuenta van a la misma particion (orden garantizado).
            kafkaTemplate.send(topic, event.numeroCuenta(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] No se pudo publicar evento {}: {}", event.eventId(), ex.getMessage());
                } else {
                    log.info("[KAFKA] Evento {} publicado en {} (particion {}, offset {})",
                            event.eventId(), topic,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("[KAFKA] Error al enviar evento {}: {}", event.eventId(), ex.getMessage());
        }
    }
}
