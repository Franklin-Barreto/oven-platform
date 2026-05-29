package br.com.f2e.ovenplatform.tenant.application.api;

import java.util.UUID;

public interface TenantLookup {

  boolean existsById(UUID tenantId);
}
