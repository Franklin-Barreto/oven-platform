package br.com.f2e.ovenplatform.catalog.application.variant;

import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository {

  ProductVariant save(ProductVariant variant);

  List<ProductVariant> saveAll(Collection<ProductVariant> variants);

  Optional<ProductVariant> findByIdAndTenantIdAndProductId(UUID id, UUID tenantId, UUID productId);

  List<ProductVariant> findByTenantIdAndProductId(UUID tenantId, UUID productId);
}
