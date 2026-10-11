package cl.duoc.batch;

import cl.duoc.batch.config.BatchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Procesos batch del Banco XYZ (migracion de los procesos legacy a Spring Batch). */
@SpringBootApplication
@EnableConfigurationProperties(BatchProperties.class)
public class BatchServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchServiceApplication.class, args);
    }
}
