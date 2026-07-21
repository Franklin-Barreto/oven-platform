package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedTenantMembership(
    UUID tenantId, UUID userId, Set<TenantMembershipRole> roles) {

  public AuthenticatedTenantMembership {
    roles = Set.copyOf(roles);
  }
}
