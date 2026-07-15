package br.com.f2e.ovenplatform.shared.domain.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

  private static final String AGGREGATE_TYPE = "TEST_AGGREGATE";
  private static final String EVENT_TYPE = "test.event";
  private static final String TEST_TOPIC = "test-events";

  @Test
  void shouldCreatePendingEventWithoutIdempotencyKey() {
    var orderId = UUID.randomUUID();

    var event =
        OutboxEvent.pending(
            AGGREGATE_TYPE,
            orderId,
            EVENT_TYPE,
            TEST_TOPIC,
            orderId.toString(),
            "{\"orderId\":\"%s\"}".formatted(orderId),
            1);

    assertThat(event.getIdempotencyKey()).isNull();
    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(event.getAttempts()).isZero();
  }

  @Test
  void shouldCreatePendingEventWithDefaultIdempotencyKey() {
    var orderId = UUID.randomUUID();

    var event =
        OutboxEvent.pendingIdempotently(
            pendingEvent(orderId, EVENT_TYPE, "{\"orderId\":\"%s\"}".formatted(orderId)));

    assertThat(event.getIdempotencyKey())
        .isEqualTo("%s:%s:%s".formatted(AGGREGATE_TYPE, orderId, EVENT_TYPE));
  }

  @Test
  void shouldAppendAdditionalPartsToIdempotencyKey() {
    var orderId = UUID.randomUUID();

    var event =
        OutboxEvent.pendingIdempotently(
            pendingEvent(orderId, "order.status.changed", "{\"status\":\"READY\"}"), "READY");

    assertThat(event.getIdempotencyKey())
        .isEqualTo("%s:%s:order.status.changed:READY".formatted(AGGREGATE_TYPE, orderId));
  }

  @Test
  void shouldRejectBlankAdditionalIdempotencyKeyPart() {
    var orderId = UUID.randomUUID();

    var event = pendingEvent(orderId, "order.status.changed", "{\"status\":\"READY\"}");

    assertThatThrownBy(() -> OutboxEvent.pendingIdempotently(event, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("idempotencyKeyPart must not be blank");
  }

  private PendingOutboxEvent pendingEvent(UUID orderId, String eventType, String payload) {
    return new PendingOutboxEvent(
        AGGREGATE_TYPE, orderId, eventType, TEST_TOPIC, orderId.toString(), payload, 1);
  }
}
