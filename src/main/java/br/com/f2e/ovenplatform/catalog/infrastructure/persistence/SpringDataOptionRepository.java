package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.domain.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOptionRepository extends JpaRepository<Option, UUID> {

  Optional<Option> findByIdAndTenantIdAndOptionGroupId(UUID id, UUID tenantId, UUID optionGroupId);

  List<Option> findByTenantIdAndOptionGroupIdOrderByDisplayPositionAscIdAsc(
      UUID tenantId, UUID optionGroupId);
}
