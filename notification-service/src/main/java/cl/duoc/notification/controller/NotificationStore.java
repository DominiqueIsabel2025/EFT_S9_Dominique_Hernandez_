package cl.duoc.notification.controller;

import cl.duoc.notification.dto.Notificacion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/** Almacen en memoria de las ultimas 100 notificaciones (suficiente para la demo). */
@Component
public class NotificationStore {

    private static final int MAX = 100;
    private final LinkedList<Notificacion> items = new LinkedList<>();

    public synchronized void agregar(Notificacion n) {
        items.addFirst(n);
        if (items.size() > MAX) {
            items.removeLast();
        }
    }

    public synchronized List<Notificacion> listar() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }
}
