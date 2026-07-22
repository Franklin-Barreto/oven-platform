package br.com.f2e.ovenplatform.identity.application.port;

import java.util.UUID;

public interface TenantValidator {
  void ensureTenantExists(UUID tenantId);
}
