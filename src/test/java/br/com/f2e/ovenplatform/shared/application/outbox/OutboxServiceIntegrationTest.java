package br.com.f2e.ovenplatform.shared.application.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.PAYLOAD_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence.JpaOutboxEventRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({OutboxService.class, JpaOutboxEventRepository.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final String FULFILLMENT_TOPIC = "fulfillment-events";
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Autowired private OutboxService outboxService;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  void shouldReturnFalseWhenIdempotentEventAlreadyExists() {
    var orderId = UUID.randomUUID();

    var firstResult = enqueueFulfillmentOrderReady(orderId, READY_AT);
    var secondResult = enqueueFulfillmentOrderReady(orderId, Instant.parse("2026-05-12T20:45:00Z"));

    assertThat(firstResult).isTrue();
    assertThat(secondResult).isFalse();
    assertThat(countOutboxEvents(orderId)).isOne();

    var event = findOutboxEvent(orderId);

    assertThat(event.getIdempotencyKey())
        .isEqualTo("%s:%s:%s".formatted(AGGREGATE_TYPE, orderId, FULFILLMENT_ORDER_READY_EVENT));
    assertThat(event.getPayload()).contains(READY_AT.toString());
  }

  @Test
  void shouldCreateOnlyOneEventWhenConcurrentAttemptsUseSameIdempotencyKey() throws Exception {
    var orderId = UUID.randomUUID();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var ready = new CountDownLatch(2);
      var start = new CountDownLatch(1);

      try {
        List<Future<Boolean>> futures =
            List.of(
                executor.submit(() -> enqueueConcurrently(orderId, READY_AT, ready, start)),
                executor.submit(
                    () ->
                        enqueueConcurrently(
                            orderId, Instant.parse("2026-05-12T20:45:00Z"), ready, start)));

        ready.await();
        start.countDown();

        var results = futures.stream().map(this::getResult).toList();

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(countOutboxEvents(orderId)).isOne();
      } finally {
        executor.shutdownNow();
      }
    }
  }

  private boolean enqueueConcurrently(
      UUID orderId, Instant readyAt, CountDownLatch ready, CountDownLatch start)
      throws InterruptedException {
    ready.countDown();
    start.await();

    return Boolean.TRUE.equals(
        new TransactionTemplate(transactionManager)
            .execute(_ -> enqueueFulfillmentOrderReady(orderId, readyAt)));
  }

  private boolean enqueueFulfillmentOrderReady(UUID orderId, Instant readyAt) {
    return outboxService.enqueueIdempotently(
        new EnqueueOutboxEventCommand(
            AGGREGATE_TYPE,
            orderId,
            FULFILLMENT_ORDER_READY_EVENT,
            FULFILLMENT_TOPIC,
            orderId.toString(),
            new FulfillmentOrderReadyTestPayload(orderId, readyAt),
            PAYLOAD_VERSION));
  }

  private long countOutboxEvents(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select count(event)
            from OutboxEvent event
            where event.aggregateType = :aggregateType
              and event.aggregateId = :aggregateId
              and event.eventType = :eventType
            """,
            Long.class)
        .setParameter("aggregateType", AGGREGATE_TYPE)
        .setParameter("aggregateId", orderId)
        .setParameter("eventType", FULFILLMENT_ORDER_READY_EVENT)
        .getSingleResult();
  }

  private OutboxEvent findOutboxEvent(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select event
            from OutboxEvent event
            where event.aggregateType = :aggregateType
              and event.aggregateId = :aggregateId
              and event.eventType = :eventType
            """,
            OutboxEvent.class)
        .setParameter("aggregateType", AGGREGATE_TYPE)
        .setParameter("aggregateId", orderId)
        .setParameter("eventType", FULFILLMENT_ORDER_READY_EVENT)
        .getSingleResult();
  }

  private boolean getResult(Future<Boolean> future) {
    try {
      return future.get();
    } catch (Exception exception) {
      throw new AssertionError("Could not get concurrent enqueue result", exception);
    }
  }

  private record FulfillmentOrderReadyTestPayload(UUID orderId, Instant readyAt) {}
}
