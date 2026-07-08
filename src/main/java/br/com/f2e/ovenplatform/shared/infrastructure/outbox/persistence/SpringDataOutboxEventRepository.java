package br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
      String aggregateType, UUID aggregateId, String eventType);

  List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
