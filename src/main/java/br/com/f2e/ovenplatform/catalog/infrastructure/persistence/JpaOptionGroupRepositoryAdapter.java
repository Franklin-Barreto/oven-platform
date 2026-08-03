package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOptionGroupRepositoryAdapter implements OptionGroupRepository {

  private final SpringDataOptionGroupRepository repository;

  JpaOptionGroupRepositoryAdapter(SpringDataOptionGroupRepository repository) {
    this.repository = repository;
  }

  @Override
  public OptionGroup save(OptionGroup optionGroup) {
    return repository.save(optionGroup);
  }

  @Override
  public Optional<OptionGroup> findByIdAndTenantIdAndProductId(
      UUID id, UUID tenantId, UUID productId) {
    return repository.findByIdAndTenantIdAndProductId(id, tenantId, productId);
  }

  @Override
  public List<OptionGroup> findByTenantIdAndProductId(UUID tenantId, UUID productId) {
    return repository.findByTenantIdAndProductIdOrderByDisplayPositionAscIdAsc(tenantId, productId);
  }
}
