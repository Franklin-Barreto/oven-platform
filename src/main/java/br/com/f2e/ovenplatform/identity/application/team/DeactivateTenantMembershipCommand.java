package br.com.f2e.ovenplatform.identity.application.team;

import java.util.UUID;

public record DeactivateTenantMembershipCommand(
    UUID tenantId, UUID actorUserId, UUID targetUserId) {}
