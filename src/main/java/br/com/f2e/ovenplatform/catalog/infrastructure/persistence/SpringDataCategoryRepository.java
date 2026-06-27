package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCategoryRepository extends JpaRepository<Category, UUID> {
  Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Category> findByTenantId(UUID tenantId);
}
