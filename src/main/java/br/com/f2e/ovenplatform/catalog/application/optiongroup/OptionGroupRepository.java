package br.com.f2e.ovenplatform.catalog.application.optiongroup;

import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OptionGroupRepository {

  OptionGroup save(OptionGroup optionGroup);

  Optional<OptionGroup> findByIdAndTenantIdAndProductId(UUID id, UUID tenantId, UUID productId);

  List<OptionGroup> findByTenantIdAndProductId(UUID tenantId, UUID productId);

  Optional<Integer> findMaxDisplayPosition(UUID tenantId, UUID productId);
}
