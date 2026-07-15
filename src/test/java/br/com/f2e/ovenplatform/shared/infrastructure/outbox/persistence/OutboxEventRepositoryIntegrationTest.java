package br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.PendingOutboxEvent;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;

@Import(JpaOutboxEventRepository.class)
class OutboxEventRepositoryIntegrationTest extends DataJpaIntegrationTest {

  @Value("${oven.kafka.topics.fulfillment}")
  private String fulfillmentTopic;

  @Autowired private OutboxEventRepository repository;

  @Test
  void shouldFindOnlyPendingEvents() {
    var pendingEvent = repository.save(pendingEvent());
    var publishedEvent = pendingEvent();
    publishedEvent.markAsPublished(Instant.parse("2026-06-28T20:00:00Z"));
    repository.save(publishedEvent);

    flushAndClear();

    var events = repository.findPendingEvents(10);

    assertThat(events)
        .extracting(OutboxEvent::getId)
        .containsExactly(pendingEvent.getId())
        .doesNotContain(publishedEvent.getId());
  }

  @Test
  void shouldAllowMultipleEventsWithoutIdempotencyKey() {
    repository.save(pendingEvent());
    repository.save(pendingEvent());

    flushAndClear();

    assertThat(repository.findPendingEvents(10)).hasSize(2);
  }

  @Test
  void shouldRejectDuplicatedIdempotencyKey() {
    var orderId = UUID.randomUUID();

    repository.save(idempotentEvent(orderId));
    repository.save(idempotentEvent(orderId));

    assertThatThrownBy(this::flushAndClear)
        .hasMessageContaining("uk_outbox_events_idempotency_key");
  }

  private OutboxEvent pendingEvent() {
    var orderId = UUID.randomUUID();
    return OutboxEvent.pending(
        AGGREGATE_TYPE,
        orderId,
        FULFILLMENT_ORDER_READY_EVENT,
        fulfillmentTopic,
        orderId.toString(),
        "{\"orderId\":\"%s\"}".formatted(orderId),
        1);
  }

  private OutboxEvent idempotentEvent(UUID orderId) {
    return OutboxEvent.pendingIdempotently(
        new PendingOutboxEvent(
            AGGREGATE_TYPE,
            orderId,
            FULFILLMENT_ORDER_READY_EVENT,
            fulfillmentTopic,
            orderId.toString(),
            "{\"orderId\":\"%s\"}".formatted(orderId),
            1));
  }
}
