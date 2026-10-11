package cl.duoc.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servidor de autorizacion OAuth2.0 del Banco XYZ.
 * Centraliza la autenticacion: emite access tokens JWT firmados (RS256)
 * que los demas microservicios (resource servers) validan.
 */
@SpringBootApplication
public class AuthServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}
