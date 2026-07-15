package br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence;

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
import org.springframework.context.annotation.Import;

@Import(JpaOutboxEventRepository.class)
class OutboxEventRepositoryIntegrationTest extends DataJpaIntegrationTest {

  private static final String AGGREGATE_TYPE = "TEST_AGGREGATE";
  private static final String EVENT_TYPE = "test.event";
  private static final String TEST_TOPIC = "test-events";

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
        EVENT_TYPE,
        TEST_TOPIC,
        orderId.toString(),
        "{\"orderId\":\"%s\"}".formatted(orderId),
        1);
  }

  private OutboxEvent idempotentEvent(UUID orderId) {
    return OutboxEvent.pendingIdempotently(
        new PendingOutboxEvent(
            AGGREGATE_TYPE,
            orderId,
            EVENT_TYPE,
            TEST_TOPIC,
            orderId.toString(),
            "{\"orderId\":\"%s\"}".formatted(orderId),
            1));
  }
}
