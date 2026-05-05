package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataProductRepository extends JpaRepository<Product, UUID> {
  Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Product> findByTenantIdAndActiveTrue(UUID tenantId);
}
