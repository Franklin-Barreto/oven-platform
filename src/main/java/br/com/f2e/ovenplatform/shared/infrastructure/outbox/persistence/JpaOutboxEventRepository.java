package br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOutboxEventRepository implements OutboxEventRepository {

  private final SpringDataOutboxEventRepository repository;

  JpaOutboxEventRepository(SpringDataOutboxEventRepository repository) {
    this.repository = repository;
  }

  @Override
  public OutboxEvent save(OutboxEvent event) {
    return repository.save(event);
  }

  @Override
  public List<OutboxEvent> saveAll(Iterable<OutboxEvent> events) {
    return repository.saveAll(events);
  }

  @Override
  public Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
      String aggregateType, UUID aggregateId, String eventType) {
    return repository.findByAggregateTypeAndAggregateIdAndEventType(
        aggregateType, aggregateId, eventType);
  }

  @Override
  public List<OutboxEvent> findPendingEvents(int limit) {
    return repository.findByStatusOrderByCreatedAtAsc(
        OutboxEventStatus.PENDING, PageRequest.of(0, limit));
  }
}
