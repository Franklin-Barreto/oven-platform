package br.com.f2e.ovenplatform.catalog.application.option;

import br.com.f2e.ovenplatform.catalog.domain.Option;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OptionRepository {

  Option save(Option option);

  List<Option> saveAll(Collection<Option> options);

  Optional<Option> findByIdAndTenantIdAndOptionGroupId(UUID id, UUID tenantId, UUID optionGroupId);

  List<Option> findByTenantIdAndOptionGroupId(UUID tenantId, UUID optionGroupId);
}
