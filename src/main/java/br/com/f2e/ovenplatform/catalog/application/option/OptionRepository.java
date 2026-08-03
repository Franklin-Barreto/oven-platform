package br.com.f2e.ovenplatform.catalog.application.option;

import br.com.f2e.ovenplatform.catalog.domain.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OptionRepository {

  Option save(Option option);

  Optional<Option> findByIdAndTenantIdAndOptionGroupId(UUID id, UUID tenantId, UUID optionGroupId);

  List<Option> findByTenantIdAndOptionGroupId(UUID tenantId, UUID optionGroupId);
}
