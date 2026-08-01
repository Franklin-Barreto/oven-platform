package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOptionGroupRepository extends JpaRepository<OptionGroup, UUID> {

  Optional<OptionGroup> findByIdAndTenantIdAndProductId(UUID id, UUID tenantId, UUID productId);

  List<OptionGroup> findByTenantIdAndProductIdOrderByDisplayPositionAscIdAsc(
      UUID tenantId, UUID productId);
}
