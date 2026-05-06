package br.com.f2e.ovenplatform.orders.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal subtotal;

  @SuppressWarnings("unused")
  protected OrderItem() {}

  OrderItem(Order order, UUID productId, int quantity, BigDecimal unitPrice) {
    this.order = requireNotNull(order, "order");
    this.productId = requireNotNull(productId, "productId");
    this.quantity = requirePositive(quantity, "quantity");
    this.unitPrice = requirePositive(unitPrice, "unitPrice");
    this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
  }

  public UUID getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }
}
