package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataProductRepository extends JpaRepository<Product, UUID> {
  Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id = :id and p.tenantId = :tenantId")
  Optional<Product> findByIdAndTenantIdForUpdate(
      @Param("id") UUID id, @Param("tenantId") UUID tenantId);

  List<Product> findByTenantIdAndActiveTrue(UUID tenantId);

  List<Product> findByTenantIdAndIdInAndActiveTrue(UUID tenantId, Set<UUID> productIds);
}
