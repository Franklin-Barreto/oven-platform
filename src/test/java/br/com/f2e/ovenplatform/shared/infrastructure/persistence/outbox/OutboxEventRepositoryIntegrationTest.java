package br.com.f2e.ovenplatform.shared.infrastructure.persistence.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.ORDER_CREATED_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaOutboxEventRepository.class)
class OutboxEventRepositoryIntegrationTest extends DataJpaIntegrationTest {

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

  private OutboxEvent pendingEvent() {
    var orderId = UUID.randomUUID();
    return OutboxEvent.pending(
        AGGREGATE_TYPE,
        orderId,
        ORDER_CREATED_EVENT,
        TOPIC,
        orderId.toString(),
        "{\"orderId\":\"%s\"}".formatted(orderId),
        1);
  }
}
