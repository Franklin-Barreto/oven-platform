package br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTenantMembershipStatusRequest(@NotNull TenantMembershipStatus status) {}
