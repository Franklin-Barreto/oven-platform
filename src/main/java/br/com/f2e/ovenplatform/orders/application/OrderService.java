package br.com.f2e.ovenplatform.orders.application;

import br.com.f2e.ovenplatform.orders.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderableProductProvider orderableProductProvider;

  public OrderService(
      OrderRepository orderRepository, OrderableProductProvider orderableProductProvider) {
    this.orderRepository = orderRepository;
    this.orderableProductProvider = orderableProductProvider;
  }

  public Order save(Order order) {
    return orderRepository.save(order);
  }

  public Order createOrder(UUID tenantId, CreateOrderCommand orderCommand) {
    var productIds =
        orderCommand.items().stream()
            .map(CreateOrderItemCommand::productId)
            .collect(Collectors.toSet());

    var orderableProducts =
        orderableProductProvider.findOrderableProducts(tenantId, productIds).stream()
            .collect(Collectors.toMap(OrderableProduct::productId, OrderableProduct::unitPrice));

    var order = new Order(tenantId);

    orderCommand
        .items()
        .forEach(
            item -> {
              var unitPrice = orderableProducts.get(item.productId());

              if (unitPrice == null) {
                throw new ProductNotAvailableForOrderingException(item.productId());
              }

              order.addItem(item.productId(), item.quantity(), unitPrice);
            });

    return orderRepository.save(order);
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
