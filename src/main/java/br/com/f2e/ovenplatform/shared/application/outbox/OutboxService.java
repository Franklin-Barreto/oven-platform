package br.com.f2e.ovenplatform.shared.application.outbox;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring dependency injection stores managed beans by reference.")
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

  private String serialize(Object payload) {
    try {
      return mapper.writeValueAsString(payload);
    } catch (JacksonException exception) {
      throw new OutboxPayloadSerializationException(exception);
    }
  }
}
