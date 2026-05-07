package br.com.f2e.ovenplatform.orders.infrastructure.persistence;

import br.com.f2e.ovenplatform.orders.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataOrderRepository extends JpaRepository<Order, UUID> {

  Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Order> findByTenantId(UUID tenantId);

  @Query(
      """
      select o
      from Order o
      left join fetch o.items
      where o.id = :id
        and o.tenantId = :tenantId
      """)
  Optional<Order> findByIdAndTenantIdWithItems(UUID id, UUID tenantId);
}
