package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.application.option.OptionRepository;
import br.com.f2e.ovenplatform.catalog.domain.Option;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOptionRepositoryAdapter implements OptionRepository {

  private final SpringDataOptionRepository repository;

  JpaOptionRepositoryAdapter(SpringDataOptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Option save(Option option) {
    return repository.save(option);
  }

  @Override
  public Optional<Option> findByIdAndTenantIdAndOptionGroupId(
      UUID id, UUID tenantId, UUID optionGroupId) {
    return repository.findByIdAndTenantIdAndOptionGroupId(id, tenantId, optionGroupId);
  }

  @Override
  public List<Option> findByTenantIdAndOptionGroupId(UUID tenantId, UUID optionGroupId) {
    return repository.findByTenantIdAndOptionGroupIdOrderByDisplayPositionAscIdAsc(
        tenantId, optionGroupId);
  }
}
