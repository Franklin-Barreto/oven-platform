package br.com.f2e.ovenplatform.kitchen.domain;

import static br.com.f2e.ovenplatform.kitchen.domain.TicketStatus.CANCELLED;
import static br.com.f2e.ovenplatform.kitchen.domain.TicketStatus.IN_PREPARATION;
import static br.com.f2e.ovenplatform.kitchen.domain.TicketStatus.READY;
import static br.com.f2e.ovenplatform.kitchen.domain.TicketStatus.RECEIVED;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotEmpty;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.kitchen.domain.exception.InvalidTicketStatusTransitionException;
import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kitchen_tickets")
public class Ticket extends BaseEntity {

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private UUID orderId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TicketStatus status;

  @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<TicketItem> items = new ArrayList<>();

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ready_at")
  private Instant readyAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  protected Ticket() {}

  public Ticket(UUID tenantId, UUID orderId, List<TicketItem> items) {
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.orderId = requireNotNull(orderId, "orderId");
    requireNotEmpty(items, "items").forEach(this::addSnapshotItem);
    this.status = RECEIVED;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public List<TicketItem> getItems() {
    return List.copyOf(items);
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getReadyAt() {
    return readyAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public void startPreparation(Instant occurredAt) {
    if (transitionTo(IN_PREPARATION)) {
      startedAt = occurredAt;
    }
  }

  public void markAsReady(Instant occurredAt) {
    if (transitionTo(READY)) {
      readyAt = occurredAt;
    }
  }

  public void cancel(Instant occurredAt) {
    if (transitionTo(CANCELLED)) {
      cancelledAt = occurredAt;
    }
  }

  private boolean transitionTo(TicketStatus target) {
    if (status == target) {
      return false;
    }
    if (!status.canTransitionTo(target)) {
      throw new InvalidTicketStatusTransitionException(status, target);
    }
    status = target;
    return true;
  }

  private void addSnapshotItem(TicketItem ticketItem) {
    var item = requireNotNull(ticketItem, "ticketItem");
    item.assignTo(this);
    this.items.add(item);
  }
}
