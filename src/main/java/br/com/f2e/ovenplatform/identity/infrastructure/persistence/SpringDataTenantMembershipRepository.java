package br.com.f2e.ovenplatform.identity.infrastructure.persistence;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTenantMembershipRepository
    extends JpaRepository<TenantMembership, UUID> {

  boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);

  Optional<TenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId);
}
