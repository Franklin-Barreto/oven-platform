package br.com.f2e.ovenplatform.identity.infrastructure.persistence;

import br.com.f2e.ovenplatform.identity.application.port.TenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTenantMembershipRepositoryAdapter implements TenantMembershipRepository {

  private final SpringDataTenantMembershipRepository tenantMembershipRepository;

  public JpaTenantMembershipRepositoryAdapter(
      SpringDataTenantMembershipRepository tenantMembershipRepository) {
    this.tenantMembershipRepository = tenantMembershipRepository;
  }

  @Override
  public TenantMembership save(TenantMembership tenantMembership) {
    return tenantMembershipRepository.save(tenantMembership);
  }

  @Override
  public boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId) {
    return tenantMembershipRepository.existsByUserIdAndTenantId(userId, tenantId);
  }

  @Override
  public Optional<TenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId) {
    return tenantMembershipRepository.findByUserIdAndTenantId(userId, tenantId);
  }

  @Override
  public List<TenantMembership> findAllByTenantId(UUID tenantId) {
    return tenantMembershipRepository.findAllByTenantId(tenantId);
  }
}
