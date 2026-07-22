package br.com.f2e.ovenplatform.identity.application.team;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import java.util.Set;
import java.util.UUID;

public record TenantUserResult(
    UUID userId,
    UUID tenantId,
    String email,
    Set<TenantMembershipRole> roles,
    TenantMembershipStatus status) {

  public TenantUserResult {
    roles = Set.copyOf(roles);
  }

  public static TenantUserResult from(TenantMembership tenantMembership) {
    return new TenantUserResult(
        tenantMembership.getUser().getId(),
        tenantMembership.getTenantId(),
        tenantMembership.getUser().getEmail(),
        tenantMembership.getRoles(),
        tenantMembership.getStatus());
  }
}
