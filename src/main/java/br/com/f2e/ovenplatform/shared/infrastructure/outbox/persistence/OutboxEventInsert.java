package br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import java.time.Instant;
import java.util.UUID;

record OutboxEventInsert(
    UUID id,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String topic,
    String messageKey,
    String payload,
    int payloadVersion,
    String status,
    int attempts,
    Instant createdAt,
    String idempotencyKey) {

  static OutboxEventInsert from(OutboxEvent event, Instant createdAt) {
    return new OutboxEventInsert(
        UUID.randomUUID(),
        event.getAggregateType(),
        event.getAggregateId(),
        event.getEventType(),
        event.getTopic(),
        event.getMessageKey(),
        event.getPayload(),
        event.getPayloadVersion(),
        event.getStatus().name(),
        event.getAttempts(),
        createdAt,
        event.getIdempotencyKey());
  }
}
