package br.com.f2e.ovenplatform.orders.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.orders.domain.exception.InvalidOrderStatusTransitionException;
import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

  @Column(nullable = false)
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OrderStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderServiceType serviceType;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<OrderItem> items = new ArrayList<>();

  @Column(name = "ready_at")
  private Instant readyAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @SuppressWarnings("unused")
  protected Order() {}

  public Order(UUID tenantId, OrderServiceType serviceType) {
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.status = OrderStatus.CREATED;
    this.serviceType = requireNotNull(serviceType, "serviceType");
    this.totalAmount = BigDecimal.ZERO;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void addItem(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    var item = new OrderItem(this, productId, productName, quantity, unitPrice);
    items.add(item);
    recalculateTotal();
  }

  public List<OrderItem> getItems() {
    return List.copyOf(items);
  }

  private void recalculateTotal() {
    this.totalAmount =
        items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public OrderStatus getStatus() {
    return status;
  }

  public OrderServiceType getServiceType() {
    return serviceType;
  }

  public void markAsReady(Instant occurredAt) {
    requireNotNull(occurredAt, "readyAt");
    if (transitionTo(OrderStatus.READY)) {
      readyAt = occurredAt;
    }
  }

  public void markAsDelivered(Instant occurredAt) {
    requireNotNull(occurredAt, "deliveredAt");
    if (transitionTo(OrderStatus.DELIVERED)) {
      deliveredAt = occurredAt;
    }
  }

  public void cancel(Instant occurredAt) {
    requireNotNull(occurredAt, "cancelledAt");
    if (transitionTo(OrderStatus.CANCELLED)) {
      cancelledAt = occurredAt;
    }
  }

  public Instant getReadyAt() {
    return readyAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  private boolean transitionTo(OrderStatus targetStatus) {
    if (status == targetStatus) {
      return false;
    }

    if (!status.canTransitionTo(targetStatus)) {
      throw new InvalidOrderStatusTransitionException(status, targetStatus);
    }

    status = targetStatus;
    return true;
  }
}
