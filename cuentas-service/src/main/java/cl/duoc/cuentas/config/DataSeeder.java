package cl.duoc.cuentas.config;

import cl.duoc.cuentas.model.Cuenta;
import cl.duoc.cuentas.repository.CuentaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Carga inicial de cuentas (H2 en memoria). */
@Component
public class DataSeeder implements CommandLineRunner {

    private final CuentaRepository cuentaRepository;

    public DataSeeder(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public void run(String... args) {
        cuentaRepository.saveAll(List.of(
                new Cuenta("CTA-0001", 1L, "CORRIENTE", new BigDecimal("850000"), new BigDecimal("850000"), "ACTIVA", new BigDecimal("300000")),
                new Cuenta("CTA-0002", 2L, "AHORRO", new BigDecimal("1250000"), new BigDecimal("1250000"), "ACTIVA", new BigDecimal("200000")),
                new Cuenta("CTA-0003", 3L, "CORRIENTE", new BigDecimal("5400000"), new BigDecimal("5400000"), "ACTIVA", new BigDecimal("500000"))));
        System.out.println(">>> [cuentas-service] 3 cuentas cargadas.");
    }
}
