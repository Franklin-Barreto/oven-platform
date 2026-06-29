package br.com.f2e.ovenplatform.shared.application.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.ORDER_CREATED_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.kafka.KafkaOutboxEventPublisher;
import br.com.f2e.ovenplatform.shared.infrastructure.kafka.KafkaTopicConfiguration;
import br.com.f2e.ovenplatform.shared.infrastructure.kafka.test.KafkaTestContainerConfiguration;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.outbox.JpaOutboxEventRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@Import({
  KafkaTestContainerConfiguration.class,
  OutboxPublisher.class,
  KafkaOutboxEventPublisher.class,
  KafkaTopicConfiguration.class,
  JpaOutboxEventRepository.class,
  OutboxPublisherKafkaIntegrationTest.TestConfig.class
})
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
class OutboxPublisherKafkaIntegrationTest extends DataJpaIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-06-29T12:00:00Z");

  @Autowired private ConfluentKafkaContainer kafkaContainer;

  @Autowired private OutboxPublisher outboxPublisher;

  @Autowired private OutboxEventRepository repository;

  @Test
  void shouldPublishPendingOutboxEventToKafkaAndMarkAsPublished() {
    var orderId = UUID.randomUUID();
    var payload = "{\"orderId\":\"%s\"}".formatted(orderId);

    repository.save(
        OutboxEvent.pending(
            AGGREGATE_TYPE, orderId, ORDER_CREATED_EVENT, TOPIC, orderId.toString(), payload, 1));

    try (var consumer = new KafkaConsumer<String, String>(consumerProperties())) {
      consumer.subscribe(List.of(TOPIC));

      outboxPublisher.publishPendingEvents();

      var rec = pollRecord(consumer, orderId.toString(), Duration.ofSeconds(10));

      assertThat(rec.topic()).isEqualTo(TOPIC);
      assertThat(rec.key()).isEqualTo(orderId.toString());
      assertThat(rec.value()).isEqualTo(payload);
    }

    var publishedEvent =
        repository
            .findByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, orderId, ORDER_CREATED_EVENT)
            .orElseThrow();

    assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    assertThat(publishedEvent.getPublishedAt()).isEqualTo(NOW);
    assertThat(publishedEvent.getLastError()).isNull();
    assertThat(repository.findPendingEvents(10)).isEmpty();
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

    throw new AssertionError("Expected Kafka record with key %s".formatted(key));
  }

  private Map<String, Object> consumerProperties() {
    return Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        kafkaContainer.getBootstrapServers(),
        ConsumerConfig.GROUP_ID_CONFIG,
        "outbox-publisher-test-%s".formatted(UUID.randomUUID()),
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
