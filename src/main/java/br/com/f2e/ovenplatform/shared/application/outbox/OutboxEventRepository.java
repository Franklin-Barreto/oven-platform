package br.com.f2e.ovenplatform.shared.application.outbox;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {
  OutboxEvent save(OutboxEvent event);

  boolean saveIfAbsent(OutboxEvent event);

  List<OutboxEvent> saveAll(Iterable<OutboxEvent> events);

  Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
      String aggregateType, UUID aggregateId, String eventType);

  List<OutboxEvent> findPendingEvents(int limit);
}
