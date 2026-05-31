package br.com.f2e.ovenplatform.identity.infrastructure.security.dto;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.UUID;

public record AuthenticatedUser(UUID tenantId, UUID userId, TenantMembershipRole role) {}
