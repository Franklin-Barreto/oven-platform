package br.com.f2e.ovenplatform.shared.infrastructure.persistence.outbox;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
      String aggregateType, UUID aggregateId, String eventType);
}
