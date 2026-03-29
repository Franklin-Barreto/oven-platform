package br.com.f2e.ovenplatform.tenant.application;

import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {
  Tenant save(Tenant tenant);

  Optional<Tenant> findById(UUID id);
}
