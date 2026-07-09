package br.com.f2e.ovenplatform.shared.application.outbox;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.PendingOutboxEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxService.class);

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

  public boolean enqueueIdempotently(
      EnqueueOutboxEventCommand command, String... additionalParameters) {
    requireNotNull(command, "command");

    var saved =
        eventRepository.saveIfAbsent(
            OutboxEvent.pendingIdempotently(toPendingOutboxEvent(command), additionalParameters));

    if (!saved) {
      LOGGER.info(
          "outbox event already exists for aggregateType={}, aggregateId={}, eventType={}",
          command.aggregateType(),
          command.aggregateId(),
          command.eventType());
    }

    return saved;
  }

  private PendingOutboxEvent toPendingOutboxEvent(EnqueueOutboxEventCommand command) {
    return new PendingOutboxEvent(
        command.aggregateType(),
        command.aggregateId(),
        command.eventType(),
        command.topic(),
        command.messageKey(),
        serialize(command.payload()),
        command.payloadVersion());
  }

  private String serialize(Object payload) {
    try {
      return mapper.writeValueAsString(payload);
    } catch (JacksonException exception) {
      throw new OutboxPayloadSerializationException(exception);
    }
  }
}
