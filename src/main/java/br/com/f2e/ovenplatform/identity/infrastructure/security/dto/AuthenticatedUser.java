package br.com.f2e.ovenplatform.identity.infrastructure.security.dto;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
    UUID tenantId,
    UUID userId,
    Set<TenantMembershipRole> roles,
    Set<TenantPermission> permissions) {

  public AuthenticatedUser {
    roles = Set.copyOf(roles);
    permissions = Set.copyOf(permissions);
  }
}
