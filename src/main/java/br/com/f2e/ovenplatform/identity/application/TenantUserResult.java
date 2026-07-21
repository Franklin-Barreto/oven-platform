package br.com.f2e.ovenplatform.identity.application;

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
}
