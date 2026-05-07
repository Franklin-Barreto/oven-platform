package br.com.f2e.ovenplatform.orders.application;

import br.com.f2e.ovenplatform.orders.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
  Order save(Order order);

  Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Order> findByIdAndTenantIdWithItems(UUID id, UUID tenantId);

  List<Order> findByTenantId(UUID tenantId);
}
