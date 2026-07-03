package br.com.f2e.ovenplatform.kitchen.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "kitchen_ticket_items")
public class TicketItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "kitchen_ticket_id", nullable = false)
  private Ticket ticket;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false, length = 80)
  private String productName;

  @Column(nullable = false)
  private int quantity;

  protected TicketItem() {}

  public TicketItem(UUID productId, String productName, int quantity) {
    this.productId = requireNotNull(productId, "productId");
    this.productName = requireMinimumSize(productName, "productName", 5);
    this.quantity = requirePositive(quantity, "quantity");
  }

  void assignTo(Ticket ticket) {
    this.ticket = requireNotNull(ticket, "ticket");
  }

  public UUID getId() {
    return id;
  }

  public UUID getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public int getQuantity() {
    return quantity;
  }
}
