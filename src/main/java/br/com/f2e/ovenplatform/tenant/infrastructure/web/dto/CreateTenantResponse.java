package br.com.f2e.ovenplatform.tenant.infrastructure.web.dto;

import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Status;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import java.time.Instant;
import java.util.UUID;

public record CreateTenantResponse(
    UUID id, String name, Plan plan, Status status, Instant createdAt, Instant updatedAt) {

  public static CreateTenantResponse from(Tenant tenant) {
    return new CreateTenantResponse(
        tenant.getId(),
        tenant.getName(),
        tenant.getPlan(),
        tenant.getStatus(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt());
  }
}
