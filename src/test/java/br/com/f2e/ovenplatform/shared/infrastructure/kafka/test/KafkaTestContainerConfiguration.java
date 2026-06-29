package br.com.f2e.ovenplatform.shared.infrastructure.kafka.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestContainerConfiguration {

  @Bean
  @ServiceConnection
  public ConfluentKafkaContainer kafkaContainer() {
    return new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0");
  }

  @Bean
  DynamicPropertyRegistrar kafkaProperties(ConfluentKafkaContainer kafkaContainer) {
    return registry ->
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
  }
}
