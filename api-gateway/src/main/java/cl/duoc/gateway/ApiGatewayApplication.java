package cl.duoc.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Puerta unica hacia los microservicios de negocio. Enruta por ruta (Path) y balancea
 * la carga (lb://) entre las instancias registradas en Eureka. El header Authorization
 * (JWT) se reenvia tal cual: cada microservicio valida el token por su cuenta.
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
