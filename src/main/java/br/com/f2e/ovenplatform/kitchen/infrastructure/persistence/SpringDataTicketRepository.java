package br.com.f2e.ovenplatform.kitchen.infrastructure.persistence;

import br.com.f2e.ovenplatform.kitchen.domain.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataTicketRepository extends JpaRepository<Ticket, UUID> {
  Optional<Ticket> findByIdAndTenantId(UUID id, UUID tenantId);

  @Query(
      """
      select ticket
      from Ticket ticket
      left join fetch ticket.items
      where ticket.id = :id
        and ticket.tenantId = :tenantId
      """)
  Optional<Ticket> findByIdAndTenantIdWithItems(UUID id, UUID tenantId);

  List<Ticket> findByTenantId(UUID tenantId);

  @Query(
      """
      select distinct ticket
      from Ticket ticket
      left join fetch ticket.items
      where ticket.tenantId = :tenantId
      """)
  List<Ticket> findByTenantIdWithItems(UUID tenantId);

  Optional<Ticket> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

  @Query(
      """
          select ticket
          from Ticket ticket
          left join fetch ticket.items
          where ticket.orderId = :orderId
            and ticket.tenantId = :tenantId
          """)
  Optional<Ticket> findByTenantIdAndOrderIdWithItems(UUID tenantId, UUID orderId);
}
