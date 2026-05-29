package br.com.f2e.ovenplatform.identity.application;

import static br.com.f2e.ovenplatform.identity.domain.validation.EmailNormalizer.normalize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.UUID;

public record CreateTenantUserCommand(
    UUID tenantId, String email, String rawPassword, TenantMembershipRole role) {

  public CreateTenantUserCommand {
    requireNotNull(tenantId, "tenantId");
    email = normalize(email);
    requireNotBlank(rawPassword, "rawPassword");
    requireNotNull(role, "role");
  }
}
