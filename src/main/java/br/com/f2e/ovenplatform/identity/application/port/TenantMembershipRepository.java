package br.com.f2e.ovenplatform.identity.application.port;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository {

  TenantMembership save(TenantMembership tenantMembership);

  boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);

  Optional<TenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId);

  List<TenantMembership> findAllByTenantId(UUID tenantId);
}
