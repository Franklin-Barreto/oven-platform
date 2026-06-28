package br.com.f2e.ovenplatform.shared.application.outbox;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {
  OutboxEvent save(OutboxEvent event);

  Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
      String aggregateType, UUID aggregateId, String eventType);
}
