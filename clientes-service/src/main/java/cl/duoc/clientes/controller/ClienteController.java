package cl.duoc.clientes.controller;

import cl.duoc.clientes.model.Cliente;
import cl.duoc.clientes.repository.ClienteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/legacy/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Long id) {
        return clienteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Iterable<Cliente>> listarClientes() {
        return ResponseEntity.ok(clienteRepository.findAll());
    }

    /** Alta de un nuevo cliente. */
    @PostMapping
    public ResponseEntity<?> crearCliente(@RequestBody Cliente nuevo) {
        if (nuevo.getRut() == null || nuevo.getRut().isBlank()
                || nuevo.getNombreCompleto() == null || nuevo.getNombreCompleto().isBlank()) {
            return ResponseEntity.badRequest().body("rut y nombreCompleto son obligatorios");
        }
        long siguienteId = clienteRepository.findAll().stream()
                .mapToLong(Cliente::getId).max().orElse(0L) + 1;
        nuevo.setId(siguienteId);
        if (nuevo.getSegmento() == null) {
            nuevo.setSegmento("PERSONA");
        }
        return ResponseEntity.status(201).body(clienteRepository.save(nuevo));
    }

    /** Actualizacion de datos de contacto (email / telefono). */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarContacto(@PathVariable Long id, @RequestBody Map<String, String> datos) {
        return clienteRepository.findById(id).map(c -> {
            if (datos.get("email") != null) c.setEmail(datos.get("email"));
            if (datos.get("telefono") != null) c.setTelefono(datos.get("telefono"));
            return ResponseEntity.ok(clienteRepository.save(c));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
