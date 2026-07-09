package br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  @Modifying
  @Transactional
  @Query(
      value =
          """
          insert into outbox_events (
            id,
            aggregate_type,
            aggregate_id,
            event_type,
            topic,
            message_key,
            payload,
            payload_version,
            status,
            attempts,
            created_at,
            idempotency_key
          )
          values (
            :#{#event.id},
            :#{#event.aggregateType},
            :#{#event.aggregateId},
            :#{#event.eventType},
            :#{#event.topic},
            :#{#event.messageKey},
            :#{#event.payload},
            :#{#event.payloadVersion},
            :#{#event.status},
            :#{#event.attempts},
            :#{#event.createdAt},
            :#{#event.idempotencyKey}
          )
          on conflict (idempotency_key) do nothing
          """,
      nativeQuery = true)
  int insertIfAbsent(@Param("event") OutboxEventInsert event);

  Optional<OutboxEvent> findByAggregateTypeAndAggregateIdAndEventType(
      String aggregateType, UUID aggregateId, String eventType);

  List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
