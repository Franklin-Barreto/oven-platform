package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.api.ActiveProductOption;
import br.com.f2e.ovenplatform.catalog.application.api.ActiveProductOptionConfiguration;
import br.com.f2e.ovenplatform.catalog.application.api.ActiveProductOptionGroup;
import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductOptionLookup;
import br.com.f2e.ovenplatform.catalog.application.option.OptionRepository;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.domain.Option;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CatalogProductOptionLookupService implements CatalogProductOptionLookup {

  private final ProductRepository productRepository;
  private final OptionGroupRepository optionGroupRepository;
  private final OptionRepository optionRepository;

  public CatalogProductOptionLookupService(
      ProductRepository productRepository,
      OptionGroupRepository optionGroupRepository,
      OptionRepository optionRepository) {
    this.productRepository = productRepository;
    this.optionGroupRepository = optionGroupRepository;
    this.optionRepository = optionRepository;
  }

  @Override
  public Optional<ActiveProductOptionConfiguration> findActiveProductOptionConfiguration(
      UUID tenantId, UUID productId) {
    if (productRepository
        .findActiveByTenantIdAndIdIn(tenantId, java.util.Set.of(productId))
        .isEmpty()) {
      return Optional.empty();
    }

    List<ActiveProductOptionGroup> optionGroups =
        optionGroupRepository.findByTenantIdAndProductId(tenantId, productId).stream()
            .filter(OptionGroup::isActive)
            .map(optionGroup -> toActiveOptionGroup(tenantId, optionGroup))
            .toList();

    return Optional.of(new ActiveProductOptionConfiguration(productId, optionGroups));
  }

  private ActiveProductOptionGroup toActiveOptionGroup(UUID tenantId, OptionGroup optionGroup) {
    List<ActiveProductOption> options =
        optionRepository.findByTenantIdAndOptionGroupId(tenantId, optionGroup.getId()).stream()
            .filter(Option::isActive)
            .map(
                option ->
                    new ActiveProductOption(
                        option.getId(), option.getName(), option.getPriceAdjustment()))
            .toList();

    return new ActiveProductOptionGroup(
        optionGroup.getId(),
        optionGroup.getName(),
        optionGroup.getMinimumSelections(),
        optionGroup.getMaximumSelections(),
        options);
  }
}
