package br.com.f2e.ovenplatform.kitchen.infrastructure.persistence;

import br.com.f2e.ovenplatform.kitchen.application.TicketRepository;
import br.com.f2e.ovenplatform.kitchen.domain.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTicketRepositoryAdapter implements TicketRepository {

  private final SpringDataTicketRepository repository;

  public JpaTicketRepositoryAdapter(SpringDataTicketRepository repository) {
    this.repository = repository;
  }

  @Override
  public Ticket save(Ticket ticket) {
    return repository.save(ticket);
  }

  @Override
  public Optional<Ticket> findByIdAndTenantId(UUID id, UUID tenantId) {
    return repository.findByIdAndTenantId(id, tenantId);
  }

  @Override
  public Optional<Ticket> findByIdAndTenantIdWithItems(UUID id, UUID tenantId) {
    return repository.findByIdAndTenantIdWithItems(id, tenantId);
  }

  @Override
  public List<Ticket> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }

  @Override
  public List<Ticket> findByTenantIdWithItems(UUID tenantId) {
    return repository.findByTenantIdWithItems(tenantId);
  }

  @Override
  public Optional<Ticket> findByTenantIdAndOrderId(UUID tenantId, UUID orderId) {
    return repository.findByTenantIdAndOrderId(tenantId, orderId);
  }
}
