package br.com.f2e.ovenplatform.orders.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<OrderItem> items = new ArrayList<>();

  @SuppressWarnings("unused")
  protected Order() {}

  public Order(UUID tenantId) {
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.status = OrderStatus.CREATED;
    this.totalAmount = BigDecimal.ZERO;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void addItem(UUID productId, int quantity, BigDecimal unitPrice) {
    var item = new OrderItem(this, productId, quantity, unitPrice);
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
}
