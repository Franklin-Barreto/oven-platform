package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantMembershipAuthenticationService {

  private final TenantMembershipRepository tenantMembershipRepository;

  public TenantMembershipAuthenticationService(
      TenantMembershipRepository tenantMembershipRepository) {
    this.tenantMembershipRepository = tenantMembershipRepository;
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
        membership.getTenantId(), membership.getUser().getId(), membership.getRoles());
  }
}
