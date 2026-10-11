package cl.duoc.pagos.config;

import cl.duoc.pagos.model.Transaccion;
import cl.duoc.pagos.repository.TransaccionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Historial inicial de transacciones (H2 en memoria). */
@Component
public class DataSeeder implements CommandLineRunner {

    private final TransaccionRepository transaccionRepository;

    public DataSeeder(TransaccionRepository transaccionRepository) {
        this.transaccionRepository = transaccionRepository;
    }

    @Override
    public void run(String... args) {
        transaccionRepository.saveAll(List.of(
                new Transaccion("CTA-0001", "DEPOSITO", new BigDecimal("500000"), LocalDateTime.now().minusDays(5), "Deposito sueldo", "WEB"),
                new Transaccion("CTA-0001", "PAGO", new BigDecimal("45000"), LocalDateTime.now().minusDays(4), "Pago cuenta luz", "MOVIL"),
                new Transaccion("CTA-0001", "RETIRO", new BigDecimal("100000"), LocalDateTime.now().minusDays(2), "Retiro cajero", "CAJERO"),
                new Transaccion("CTA-0001", "TRANSFERENCIA", new BigDecimal("30000"), LocalDateTime.now().minusDays(1), "Transferencia a CTA-0002", "WEB"),
                new Transaccion("CTA-0002", "DEPOSITO", new BigDecimal("1000000"), LocalDateTime.now().minusDays(10), "Deposito inicial", "WEB"),
                new Transaccion("CTA-0002", "INTERES", new BigDecimal("15000"), LocalDateTime.now().minusDays(3), "Interes mensual", "SUCURSAL"),
                new Transaccion("CTA-0002", "RETIRO", new BigDecimal("50000"), LocalDateTime.now().minusHours(6), "Retiro cajero", "CAJERO"),
                new Transaccion("CTA-0003", "DEPOSITO", new BigDecimal("2000000"), LocalDateTime.now().minusDays(7), "Deposito ventas", "WEB"),
                new Transaccion("CTA-0003", "PAGO", new BigDecimal("800000"), LocalDateTime.now().minusDays(1), "Pago proveedores", "WEB")));
        System.out.println(">>> [pagos-service] 9 transacciones iniciales cargadas.");
    }
}
