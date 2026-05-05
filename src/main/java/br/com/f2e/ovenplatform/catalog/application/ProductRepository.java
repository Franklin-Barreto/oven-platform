package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
  Product save(Product product);

  Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

  List<Product> findActiveByTenantId(UUID tenantId);
}
