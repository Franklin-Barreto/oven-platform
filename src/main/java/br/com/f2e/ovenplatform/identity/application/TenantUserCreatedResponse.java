package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import java.util.UUID;

public record TenantUserCreatedResponse(
    UUID userId, UUID tenantId, TenantMembershipRole role, TenantMembershipStatus status) {}
