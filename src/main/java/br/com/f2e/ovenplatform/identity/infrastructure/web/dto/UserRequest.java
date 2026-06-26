package br.com.f2e.ovenplatform.identity.infrastructure.web.dto;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(
    @NotBlank @Email String email, @NotBlank String password, @NotNull TenantMembershipRole role) {}
