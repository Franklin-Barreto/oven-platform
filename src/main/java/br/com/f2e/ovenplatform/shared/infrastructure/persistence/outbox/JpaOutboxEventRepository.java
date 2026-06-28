package br.com.f2e.ovenplatform.shared.infrastructure.persistence.outbox;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import java.util.Optional;
import java.util.UUID;
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
  public Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
      String aggregateType, UUID aggregateId, String eventType) {
    return repository.findByAggregateTypeAndAggregateIdAndEventType(
        aggregateType, aggregateId, eventType);
  }
}
