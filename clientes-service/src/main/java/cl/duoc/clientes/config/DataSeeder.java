package cl.duoc.clientes.config;

import cl.duoc.clientes.model.Cliente;
import cl.duoc.clientes.repository.ClienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Carga inicial de clientes (H2 en memoria). */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ClienteRepository clienteRepository;

    public DataSeeder(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void run(String... args) {
        clienteRepository.saveAll(List.of(
                new Cliente(1L, "12.345.678-9", "Maria Fernanda Soto", "mfsoto@correo.cl", "+56911111111", "PERSONA"),
                new Cliente(2L, "9.876.543-2", "Jose Luis Pizarro", "jlpizarro@correo.cl", "+56922222222", "PERSONA"),
                new Cliente(3L, "76.543.210-1", "Comercial Andes SpA", "contacto@andes.cl", "+56233333333", "PYME")));
        System.out.println(">>> [clientes-service] 3 clientes cargados.");
    }
}
