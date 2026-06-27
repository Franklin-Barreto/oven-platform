package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.application.CategoryRepository;
import br.com.f2e.ovenplatform.catalog.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

  private final SpringDataCategoryRepository repository;

  JpaCategoryRepositoryAdapter(SpringDataCategoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public Category save(Category category) {
    return repository.save(category);
  }

  @Override
  public Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId) {
    return repository.findByIdAndTenantId(id, tenantId);
  }

  @Override
  public List<Category> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }
}
