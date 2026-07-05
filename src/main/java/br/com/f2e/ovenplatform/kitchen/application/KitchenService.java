package br.com.f2e.ovenplatform.kitchen.application;

import br.com.f2e.ovenplatform.kitchen.domain.Ticket;
import br.com.f2e.ovenplatform.kitchen.domain.TicketItem;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitchenService {

  private static final String RESOURCE = "Ticket";
  private final TicketRepository repository;
  private final Clock clock;

  public KitchenService(TicketRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public Ticket createTicketFromOrder(CreateTicketCommand command) {
    return repository
        .findByTenantIdAndOrderId(command.tenantId(), command.orderId())
        .orElseGet(() -> createAndSaveTicket(command));
  }

  @Transactional(readOnly = true)
  public List<Ticket> list(UUID tenantId) {
    return repository.findByTenantIdWithItems(tenantId);
  }

  @Transactional(readOnly = true)
  public Ticket findById(UUID tenantId, UUID id) {
    return repository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }

  @Transactional(readOnly = true)
  public Ticket findByIdWithItems(UUID tenantId, UUID id) {
    return repository
        .findByIdAndTenantIdWithItems(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }

  @Transactional
  public void startPreparation(UUID tenantId, UUID id) {
    updateTicket(tenantId, id, ticket -> ticket.startPreparation(clock.instant()));
  }

  private void updateTicket(UUID tenantId, UUID id, Consumer<Ticket> action) {
    var ticket = findTicketById(tenantId, id);
    action.accept(ticket);
  }

  @Transactional
  public void markAsReady(UUID tenantId, UUID id) {
    updateTicket(tenantId, id, ticket -> ticket.markAsReady(clock.instant()));
  }

  @Transactional
  public void cancel(UUID tenantId, UUID id) {
    updateTicket(tenantId, id, ticket -> ticket.cancel(clock.instant()));
  }

  private Ticket findTicketById(UUID tenantId, UUID id) {
    return repository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }

  private Ticket createAndSaveTicket(CreateTicketCommand command) {
    var items =
        command.items().stream()
            .map(item -> new TicketItem(item.productId(), item.productName(), item.quantity()))
            .toList();

    var ticket = new Ticket(command.tenantId(), command.orderId(), items);

    return repository.save(ticket);
  }

  @Transactional(readOnly = true)
  public Ticket findByOrderIdWithItems(UUID tenantId, UUID orderId) {
    return repository
        .findByTenantIdAndOrderIdWithItems(tenantId, orderId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, "orderId", orderId));
  }
}
