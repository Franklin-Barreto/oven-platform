package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOptionGroupRepository extends JpaRepository<OptionGroup, UUID> {

  Optional<OptionGroup> findByIdAndTenantIdAndProductId(UUID id, UUID tenantId, UUID productId);

  List<OptionGroup> findByTenantIdAndProductIdOrderByDisplayPositionAscIdAsc(
      UUID tenantId, UUID productId);

  @Query(
      """
      select max(og.displayPosition)
      from OptionGroup og
      where og.tenantId = :tenantId and og.productId = :productId
      """)
  Optional<Integer> findMaxDisplayPosition(
      @Param("tenantId") UUID tenantId, @Param("productId") UUID productId);
}
