package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository {

  TenantMembership save(TenantMembership tenantMembership);

  boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);

  Optional<TenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId);
}
