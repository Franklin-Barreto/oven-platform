package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantRepository;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProductVariantRepositoryAdapter implements ProductVariantRepository {

  private final SpringDataProductVariantRepository repository;

  JpaProductVariantRepositoryAdapter(SpringDataProductVariantRepository repository) {
    this.repository = repository;
  }

  @Override
  public ProductVariant save(ProductVariant variant) {
    return repository.save(variant);
  }

  @Override
  public List<ProductVariant> saveAll(Collection<ProductVariant> variants) {
    return repository.saveAll(variants);
  }

  @Override
  public Optional<ProductVariant> findByIdAndTenantIdAndProductId(
      UUID id, UUID tenantId, UUID productId) {
    return repository.findByIdAndTenantIdAndProductId(id, tenantId, productId);
  }

  @Override
  public List<ProductVariant> findByTenantIdAndProductId(UUID tenantId, UUID productId) {
    return repository.findByTenantIdAndProductIdOrderByDisplayPositionAscIdAsc(tenantId, productId);
  }

  @Override
  public List<ProductVariant> findByTenantIdAndProductIds(UUID tenantId, Set<UUID> productIds) {
    return repository.findByTenantIdAndProductIdInOrderByProductIdAscDisplayPositionAscIdAsc(
        tenantId, productIds);
  }
}
