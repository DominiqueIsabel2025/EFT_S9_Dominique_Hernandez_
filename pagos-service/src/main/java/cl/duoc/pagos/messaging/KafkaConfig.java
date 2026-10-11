package cl.duoc.pagos.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    /** El topic se crea automaticamente al arrancar (via KafkaAdmin) si no existe. */
    @Bean
    public NewTopic transaccionesTopic(@Value("${app.kafka.topic-transacciones}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
