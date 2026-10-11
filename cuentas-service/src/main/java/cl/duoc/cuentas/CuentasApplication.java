package cl.duoc.cuentas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio de Gestion de Cuentas: apertura, cierre, bloqueo y movimiento de saldos. */
@SpringBootApplication
public class CuentasApplication {
    public static void main(String[] args) {
        SpringApplication.run(CuentasApplication.class, args);
    }
}
