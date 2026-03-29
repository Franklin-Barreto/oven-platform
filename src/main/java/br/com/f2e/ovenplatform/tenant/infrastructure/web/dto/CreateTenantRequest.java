package br.com.f2e.ovenplatform.tenant.infrastructure.web.dto;

import br.com.f2e.ovenplatform.tenant.domain.Plan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantRequest(@NotBlank String name, @NotNull Plan plan) {}
