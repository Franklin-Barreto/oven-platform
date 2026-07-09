package br.com.f2e.ovenplatform.shared.domain.outbox;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import java.util.UUID;

public record PendingOutboxEvent(
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String topic,
    String messageKey,
    String payload,
    int payloadVersion) {

  public PendingOutboxEvent {
    requireNotBlank(aggregateType, "aggregateType");
    requireNotNull(aggregateId, "aggregateId");
    requireNotBlank(eventType, "eventType");
    requireNotBlank(topic, "topic");
    requireNotBlank(messageKey, "messageKey");
    requireNotBlank(payload, "payload");
    requirePositive(payloadVersion, "payloadVersion");
  }
}
