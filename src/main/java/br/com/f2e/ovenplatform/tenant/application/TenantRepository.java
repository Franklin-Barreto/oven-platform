package br.com.f2e.ovenplatform.tenant.application;

import br.com.f2e.ovenplatform.tenant.domain.Tenant;

public interface TenantRepository {
  Tenant save(Tenant tenant);
}
