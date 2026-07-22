package br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record ReplaceTenantMembershipRolesRequest(
    @NotEmpty Set<@NotNull TenantMembershipRole> roles) {

  public ReplaceTenantMembershipRolesRequest {
    if (roles != null) {
      roles = Collections.unmodifiableSet(new HashSet<>(roles));
    }
  }

  @Override
  public Set<TenantMembershipRole> roles() {
    return roles == null ? null : Set.copyOf(roles);
  }
}
