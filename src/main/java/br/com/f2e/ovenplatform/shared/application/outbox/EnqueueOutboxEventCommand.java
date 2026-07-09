package br.com.f2e.ovenplatform.shared.application.outbox;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import java.util.UUID;

public record EnqueueOutboxEventCommand(
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String topic,
    String messageKey,
    Object payload,
    int payloadVersion) {

  public EnqueueOutboxEventCommand {
     requireNotBlank(aggregateType, "aggregateType");
    requireNotNull(aggregateId, "aggregateId");
    requireNotBlank(eventType, "eventType");
    requireNotBlank(topic, "topic");
    requireNotBlank(messageKey, "messageKey");
    requireNotNull(payload, "payload");
    requirePositive(payloadVersion, "payloadVersion");
  }
}
