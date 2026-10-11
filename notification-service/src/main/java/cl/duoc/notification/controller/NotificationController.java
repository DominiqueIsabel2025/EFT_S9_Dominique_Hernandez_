package cl.duoc.notification.controller;

import cl.duoc.notification.dto.Notificacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationStore store;

    public NotificationController(NotificationStore store) {
        this.store = store;
    }

    /** Ultimas notificaciones recibidas via Kafka (mas recientes primero). */
    @GetMapping
    public List<Notificacion> listar() {
        return store.listar();
    }
}
