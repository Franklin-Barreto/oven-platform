package br.com.f2e.ovenplatform.identity.application.team;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Set;
import java.util.UUID;

public record ReplaceTenantMembershipRolesCommand(
    UUID tenantId, UUID actorUserId, UUID targetUserId, Set<TenantMembershipRole> roles) {

  public ReplaceTenantMembershipRolesCommand {
    roles = Set.copyOf(roles);
  }
}
