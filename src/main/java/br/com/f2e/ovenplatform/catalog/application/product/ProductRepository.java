package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProductRepository {
  Product save(Product product);

  Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Product> findByIdAndTenantIdForUpdate(UUID id, UUID tenantId);

  List<Product> findActiveByTenantId(UUID tenantId);

  List<Product> findActiveByTenantIdAndIdIn(UUID tenantId, Set<UUID> productIds);
}
