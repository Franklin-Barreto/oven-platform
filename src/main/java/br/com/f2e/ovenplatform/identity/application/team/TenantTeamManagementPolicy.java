package br.com.f2e.ovenplatform.identity.application.team;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TenantTeamManagementPolicy {

  public void ensureCanChangeRoles(
      Set<TenantMembershipRole> actorRoles,
      Set<TenantMembershipRole> currentTargetRoles,
      Set<TenantMembershipRole> requestedRoles) {

    ensureCanManageMembership(actorRoles, currentTargetRoles);
    ensureCanManageMembership(actorRoles, requestedRoles);
  }

  public void ensureCanManageMembership(
      Set<TenantMembershipRole> actorRoles, Set<TenantMembershipRole> targetRoles) {

    if (!canManageAll(actorRoles, targetRoles)) {
      throw new TenantTeamManagementDeniedException();
    }
  }

  private boolean canManageAll(
      Set<TenantMembershipRole> actorRoles, Set<TenantMembershipRole> targetRoles) {

    return actorRoles.stream()
        .anyMatch(actorRole -> targetRoles.stream().allMatch(actorRole::canManage));
  }
}
