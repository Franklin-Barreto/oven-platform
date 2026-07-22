package br.com.f2e.ovenplatform.identity.application.team;

import java.util.UUID;

public record ReactivateTenantMembershipCommand(
    UUID tenantId, UUID actorUserId, UUID targetUserId) {}
