package br.com.f2e.ovenplatform.shared.domain.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.ORDER_CREATED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

  private static final String ORDER_TOPIC = "order-events";

  @Test
  void shouldCreatePendingEventWithoutIdempotencyKey() {
    var orderId = UUID.randomUUID();

    var event =
        OutboxEvent.pending(
            AGGREGATE_TYPE,
            orderId,
            ORDER_CREATED_EVENT,
            ORDER_TOPIC,
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
            pendingEvent(orderId, ORDER_CREATED_EVENT, "{\"orderId\":\"%s\"}".formatted(orderId)));

    assertThat(event.getIdempotencyKey())
        .isEqualTo("%s:%s:%s".formatted(AGGREGATE_TYPE, orderId, ORDER_CREATED_EVENT));
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
        AGGREGATE_TYPE, orderId, eventType, ORDER_TOPIC, orderId.toString(), payload, 1);
  }
}
