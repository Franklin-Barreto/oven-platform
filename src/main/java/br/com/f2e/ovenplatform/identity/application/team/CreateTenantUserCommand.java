package br.com.f2e.ovenplatform.identity.application.team;

import static br.com.f2e.ovenplatform.identity.domain.validation.EmailNormalizer.normalize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotEmptyAndWithoutNulls;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Set;
import java.util.UUID;

public record CreateTenantUserCommand(
    UUID tenantId,
    UUID actorUserId,
    String email,
    String rawPassword,
    Set<TenantMembershipRole> roles) {

  public CreateTenantUserCommand {
    requireNotNull(tenantId, "tenantId");
    requireNotNull(actorUserId, "actorUserId");
    email = normalize(email);
    requireNotBlank(rawPassword, "rawPassword");
    roles = Set.copyOf(requireNotEmptyAndWithoutNulls(roles, "roles"));
  }
}
