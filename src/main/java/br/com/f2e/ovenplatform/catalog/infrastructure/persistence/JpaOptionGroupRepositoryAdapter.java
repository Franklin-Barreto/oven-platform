package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

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
  public List<OptionGroup> saveAll(Collection<OptionGroup> optionGroups) {
    return repository.saveAll(optionGroups);
  }

  @Override
  public Optional<OptionGroup> findByIdAndProductId(UUID id, UUID productId) {
    return repository.findByIdAndProductId(id, productId);
  }

  @Override
  public List<OptionGroup> findByProductId(UUID productId) {
    return repository.findByProductIdOrderByDisplayPositionAscIdAsc(productId);
  }
}
