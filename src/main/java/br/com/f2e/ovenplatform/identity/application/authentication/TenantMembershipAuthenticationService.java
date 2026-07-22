package br.com.f2e.ovenplatform.identity.application.authentication;

import br.com.f2e.ovenplatform.identity.application.port.TenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.application.security.TenantPermissionResolver;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantMembershipAuthenticationService {

  private final TenantMembershipRepository tenantMembershipRepository;
  private final TenantPermissionResolver permissionResolver;

  public TenantMembershipAuthenticationService(
      TenantMembershipRepository tenantMembershipRepository,
      TenantPermissionResolver permissionResolver) {
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.permissionResolver = permissionResolver;
  }

  @Transactional(readOnly = true)
  public AuthenticatedTenantMembership loadActiveMembership(UUID userId, UUID tenantId) {
    var membership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(userId, tenantId)
            .orElseThrow(TenantAccessDeniedException::new);

    if (membership.getStatus() != TenantMembershipStatus.ACTIVE) {
      throw new TenantMembershipInactiveException();
    }

    return new AuthenticatedTenantMembership(
        membership.getTenantId(),
        membership.getUser().getId(),
        membership.getRoles(),
        permissionResolver.resolve(membership.getRoles()));
  }
}
