package br.com.f2e.ovenplatform.identity.infrastructure.persistence;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataTenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

  boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
}
