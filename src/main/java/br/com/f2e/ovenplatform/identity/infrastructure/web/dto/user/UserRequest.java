package br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UserRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotEmpty Set<@NotNull TenantMembershipRole> roles) {

  public UserRequest {
    roles = roles == null ? null : Set.copyOf(roles);
  }
}
