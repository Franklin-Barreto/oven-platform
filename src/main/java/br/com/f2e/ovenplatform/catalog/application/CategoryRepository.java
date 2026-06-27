package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

  Category save(Category category);

  Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Category> findByTenantId(UUID tenantId);
}
