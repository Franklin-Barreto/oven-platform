package br.com.f2e.ovenplatform.kitchen.application;

import br.com.f2e.ovenplatform.kitchen.domain.Ticket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {

  Ticket save(Ticket ticket);

  Optional<Ticket> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Ticket> findByIdAndTenantIdWithItems(UUID id, UUID tenantId);

  List<Ticket> findByTenantId(UUID tenantId);

  List<Ticket> findByTenantIdWithItems(UUID tenantId);
}
