package br.com.f2e.ovenplatform.identity.application.authentication;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedTenantMembership(
    UUID tenantId,
    UUID userId,
    Set<TenantMembershipRole> roles,
    Set<TenantPermission> permissions) {

  public AuthenticatedTenantMembership {
    roles = Set.copyOf(roles);
    permissions = Set.copyOf(permissions);
  }
}
