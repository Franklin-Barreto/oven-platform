package br.com.f2e.ovenplatform.orders.infrastructure.persistence;

import br.com.f2e.ovenplatform.orders.application.OrderRepository;
import br.com.f2e.ovenplatform.orders.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

  private final SpringDataOrderRepository orderRepository;

  public JpaOrderRepositoryAdapter(SpringDataOrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  @Override
  public Order save(Order order) {
    return orderRepository.save(order);
  }

  @Override
  public Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId) {
    return orderRepository.findByIdAndTenantId(id, tenantId);
  }

  @Override
  public Optional<Order> findByIdAndTenantIdWithItems(UUID id, UUID tenantId) {
    return orderRepository.findByIdAndTenantIdWithItems(id, tenantId);
  }

  @Override
  public List<Order> findByTenantId(UUID tenantId) {
    return orderRepository.findByTenantId(tenantId);
  }
}
