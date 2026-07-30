package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

  Optional<ProductVariant> findByIdAndTenantIdAndProductId(UUID id, UUID tenantId, UUID productId);

  List<ProductVariant> findByTenantIdAndProductIdOrderByDisplayPositionAscIdAsc(
      UUID tenantId, UUID productId);

  List<ProductVariant> findByTenantIdAndProductIdInOrderByProductIdAscDisplayPositionAscIdAsc(
      UUID tenantId, Collection<UUID> productIds);
}
