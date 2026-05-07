package br.com.f2e.ovenplatform.orders.application;

import br.com.f2e.ovenplatform.orders.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

  private final OrderRepository orderRepository;

  public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  public Order save(Order order) {
    return orderRepository.save(order);
  }

  public Order createOrder(UUID tenantId) {
    return orderRepository.save(new Order(tenantId));
  }

  public Optional<Order> findOrder(UUID tenantId, UUID orderId) {
    return orderRepository.findByIdAndTenantId(orderId, tenantId);
  }

  public Optional<Order> findOrderWithItems(UUID tenantId, UUID orderId) {
    return orderRepository.findByIdAndTenantIdWithItems(orderId, tenantId);
  }

  public List<Order> listOrders(UUID tenantId) {
    return orderRepository.findByTenantId(tenantId);
  }
}
