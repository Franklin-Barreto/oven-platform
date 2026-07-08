package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.infrastructure.kafka.OrderCreatedKitchenTicketConsumer;
import br.com.f2e.ovenplatform.shared.infrastructure.kafka.test.KafkaTestContainerConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@SpringBootTest(
    classes = {
      KafkaTestContainerConfiguration.class,
      KafkaTopicConfiguration.class,
      KafkaConsumerErrorHandlingConfiguration.class,
      OrderCreatedKitchenTicketConsumer.class,
      KafkaConsumerErrorHandlingIntegrationTest.TestConfig.class
    })
@ImportAutoConfiguration({KafkaAutoConfiguration.class, JacksonAutoConfiguration.class})
@ActiveProfiles("test")
class KafkaConsumerErrorHandlingIntegrationTest {

  private static final String DEAD_LETTER_TOPIC_SUFFIX = "-dlt";

  @Value("${oven.kafka.topics.orders}")
  private String orderTopic;

  @Autowired private ConfluentKafkaContainer kafkaContainer;

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired private KitchenService kitchenService;

  @Test
  void shouldSendMessageToDeadLetterTopicWithoutRetryingIllegalArgumentException()
      throws Exception {
    var orderId = UUID.randomUUID();
    var payload = orderCreatedPayload(orderId);

    doThrow(new IllegalArgumentException("Invalid order contract"))
        .when(kitchenService)
        .createTicketFromOrder(any());

    try (var consumer = new KafkaConsumer<String, String>(consumerProperties())) {
      consumer.subscribe(List.of(orderTopic + DEAD_LETTER_TOPIC_SUFFIX));

      kafkaTemplate.send(orderTopic, orderId.toString(), payload).get(10, TimeUnit.SECONDS);

      var deadLetterRecord = pollRecord(consumer, orderId.toString(), Duration.ofSeconds(10));

      assertThat(deadLetterRecord.topic()).isEqualTo(orderTopic + DEAD_LETTER_TOPIC_SUFFIX);
      assertThat(deadLetterRecord.key()).isEqualTo(orderId.toString());
      assertThat(deadLetterRecord.value()).isEqualTo(payload);
    }

    verify(kitchenService).createTicketFromOrder(any());
  }

  private ConsumerRecord<String, String> pollRecord(
      KafkaConsumer<String, String> consumer, String key, Duration timeout) {
    var deadline = System.nanoTime() + timeout.toNanos();

    while (System.nanoTime() < deadline) {
      var records = consumer.poll(Duration.ofMillis(500));

      for (var rec : records) {
        if (key.equals(rec.key())) {
          return rec;
        }
      }
    }

    throw new AssertionError("Expected Kafka dead letter record with key %s".formatted(key));
  }

  private Map<String, Object> consumerProperties() {
    return Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        kafkaContainer.getBootstrapServers(),
        ConsumerConfig.GROUP_ID_CONFIG,
        "kafka-error-handling-test-%s".formatted(UUID.randomUUID()),
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
  }

  private String orderCreatedPayload(UUID orderId) {
    return """
        {
          "tenantId": "a6210129-f1d5-4942-8d0a-b144e518aecc",
          "orderId": "%s",
          "totalAmount": 120.00,
          "paymentMethod": "CARD",
          "paymentStatus": "PENDING",
          "items": [
            {
              "productId": "b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234",
              "productName": "Pizza Portuguesa",
              "quantity": 2,
              "unitPrice": 60.00
            }
          ]
        }
        """
        .formatted(orderId);
  }

  @EnableKafka
  @TestConfiguration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    KitchenService kitchenService() {
      return mock(KitchenService.class);
    }
  }
}
