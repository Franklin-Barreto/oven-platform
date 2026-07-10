package br.com.f2e.ovenplatform.orders.application;

import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentMarkedAsPaidEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private static final String RESOURCE = "Order";

  private final OrderRepository orderRepository;
  private final OrderableProductProvider orderableProductProvider;
  private final Clock clock;
  private final ApplicationEventPublisher eventPublisher;
  private final OrderCreatedEventPublisher orderCreatedEventPublisher;

  public OrderService(
      OrderRepository orderRepository,
      OrderableProductProvider orderableProductProvider,
      Clock clock,
      ApplicationEventPublisher eventPublisher,
      OrderCreatedEventPublisher orderCreatedEventPublisher) {
    this.orderRepository = orderRepository;
    this.orderableProductProvider = orderableProductProvider;
    this.clock = clock;
    this.eventPublisher = eventPublisher;
    this.orderCreatedEventPublisher = orderCreatedEventPublisher;
  }

  public Order save(Order order) {
    return orderRepository.save(order);
  }

  @Transactional
  public Order createOrder(UUID tenantId, CreateOrderCommand orderCommand) {
    var productIds =
        orderCommand.items().stream()
            .map(CreateOrderItemCommand::productId)
            .collect(Collectors.toSet());

    var orderableProducts =
        orderableProductProvider.findOrderableProducts(tenantId, productIds).stream()
            .collect(Collectors.toMap(OrderableProduct::productId, product -> product));

    var order = new Order(tenantId, orderCommand.serviceType());

    orderCommand
        .items()
        .forEach(
            item -> {
              var orderableProduct = orderableProducts.get(item.productId());

              if (orderableProduct == null) {
                throw new ProductNotAvailableForOrderingException(item.productId());
              }

              order.addItem(
                  item.productId(),
                  orderableProduct.productName(),
                  item.quantity(),
                  orderableProduct.unitPrice());
            });

    var savedOrder = orderRepository.save(order);

    var orderCreatedEvent =
        new OrderCreatedEvent(
            savedOrder.getTenantId(),
            savedOrder.getId(),
            orderCommand.paymentInfo().method(),
            orderCommand.paymentInfo().status(),
            savedOrder.getTotalAmount(),
            savedOrder.getItems().stream().map(OrderPlacedItem::from).toList());

    eventPublisher.publishEvent(orderCreatedEvent);
    orderCreatedEventPublisher.publish(orderCreatedEvent);

    return savedOrder;
  }

  @Transactional(readOnly = true)
  public Optional<Order> findOrder(UUID tenantId, UUID orderId) {
    return orderRepository.findByIdAndTenantId(orderId, tenantId);
  }

  @Transactional(readOnly = true)
  public Optional<Order> findOrderWithItems(UUID tenantId, UUID orderId) {
    return orderRepository.findByIdAndTenantIdWithItems(orderId, tenantId);
  }

  @Transactional(readOnly = true)
  public List<Order> listOrders(UUID tenantId) {
    return orderRepository.findByTenantId(tenantId);
  }

  @Transactional
  public void markAsReady(UUID tenantId, UUID orderId) {
    updateOrder(tenantId, orderId, order -> order.markAsReady(clock.instant()));
  }

  @Transactional
  public void markAsReady(UUID tenantId, UUID orderId, Instant readyAt) {
    updateOrder(tenantId, orderId, order -> order.markAsReady(readyAt));
  }

  @Transactional
  public void markAsDelivered(UUID tenantId, UUID orderId) {
    updateOrder(tenantId, orderId, order -> order.markAsDelivered(clock.instant()));
  }

  @Transactional
  public void cancel(UUID tenantId, UUID orderId) {
    updateOrder(tenantId, orderId, order -> order.cancel(clock.instant()));
  }

  @Transactional
  public void markPaymentAsPaid(UUID tenantId, UUID orderId) {
    var order =
        orderRepository
            .findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, orderId));
    eventPublisher.publishEvent(
        new OrderPaymentMarkedAsPaidEvent(order.getTenantId(), order.getId()));
  }

  private void updateOrder(UUID tenantId, UUID orderId, Consumer<Order> operation) {
    var order =
        orderRepository
            .findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, orderId));

    operation.accept(order);
  }
}
