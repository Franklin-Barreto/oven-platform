package br.com.f2e.ovenplatform.tenant.infrastructure.web.dto;

import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Status;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
    UUID id, String name, Plan plan, Status status, Instant createdAt, Instant updatedAt) {

  public static TenantResponse from(Tenant tenant) {
    return new TenantResponse(
        tenant.getId(),
        tenant.getName(),
        tenant.getPlan(),
        tenant.getStatus(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt());
  }
}
