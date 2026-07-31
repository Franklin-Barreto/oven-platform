package br.com.f2e.ovenplatform.catalog.application.optiongroup;

import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OptionGroupRepository {

  OptionGroup save(OptionGroup optionGroup);

  List<OptionGroup> saveAll(Collection<OptionGroup> optionGroups);

  Optional<OptionGroup> findByIdAndProductId(UUID id, UUID productId);

  List<OptionGroup> findByProductId(UUID productId);
}
