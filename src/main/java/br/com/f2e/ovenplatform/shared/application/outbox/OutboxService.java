package br.com.f2e.ovenplatform.shared.application.outbox;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxService {

  private final OutboxEventRepository eventRepository;
  private final ObjectMapper mapper;

  public OutboxService(OutboxEventRepository eventRepository, ObjectMapper mapper) {
    this.eventRepository = eventRepository;
    this.mapper = mapper;
  }

  public void enqueue(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String topic,
      String messageKey,
      Object payload,
      int payloadVersion) {

    eventRepository.save(
        OutboxEvent.pending(
            aggregateType,
            aggregateId,
            eventType,
            topic,
            messageKey,
            serialize(payload),
            payloadVersion));
  }

  public boolean enqueueIfAbsent(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String topic,
      String messageKey,
      Object payload,
      int payloadVersion) {
    var existingEvent =
        eventRepository.findByAggregateTypeAndAggregateIdAndEventType(
            aggregateType, aggregateId, eventType);

    if (existingEvent.isPresent()) {
      return false;
    }

    enqueue(aggregateType, aggregateId, eventType, topic, messageKey, payload, payloadVersion);
    return true;
  }

  private String serialize(Object payload) {
    try {
      return mapper.writeValueAsString(payload);
    } catch (JacksonException exception) {
      throw new OutboxPayloadSerializationException(exception);
    }
  }
}
