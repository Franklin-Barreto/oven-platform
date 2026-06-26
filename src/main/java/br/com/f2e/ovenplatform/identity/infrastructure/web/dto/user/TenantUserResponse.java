package br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user;

import br.com.f2e.ovenplatform.identity.application.TenantUserResult;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import java.util.UUID;

public record TenantUserResponse(
    UUID id,
    UUID tenantId,
    String email,
    TenantMembershipRole role,
    TenantMembershipStatus status) {

  public static TenantUserResponse from(TenantUserResult result) {
    return new TenantUserResponse(
        result.userId(), result.tenantId(), result.email(), result.role(), result.status());
  }
}
