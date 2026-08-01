package br.com.f2e.ovenplatform.catalog.application.option;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.Option;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptionService {

  private static final String PRODUCT_RESOURCE = "Product";
  private static final String OPTION_GROUP_RESOURCE = "OptionGroup";
  private static final String OPTION_RESOURCE = "Option";

  private final ProductRepository productRepository;
  private final OptionGroupRepository optionGroupRepository;
  private final OptionRepository optionRepository;

  public OptionService(
      ProductRepository productRepository,
      OptionGroupRepository optionGroupRepository,
      OptionRepository optionRepository) {
    this.productRepository = productRepository;
    this.optionGroupRepository = optionGroupRepository;
    this.optionRepository = optionRepository;
  }

  @Transactional
  public OptionResult create(
      UUID tenantId, UUID productId, UUID optionGroupId, CreateOptionCommand command) {
    requireProduct(tenantId, productId);
    requireOptionGroup(tenantId, productId, optionGroupId);

    int nextDisplayPosition =
        optionRepository.findByTenantIdAndOptionGroupId(tenantId, optionGroupId).stream()
                .mapToInt(Option::getDisplayPosition)
                .max()
                .orElse(-1)
            + 1;
    Option option =
        new Option(
            optionGroupId,
            tenantId,
            command.name(),
            command.priceAdjustment(),
            nextDisplayPosition);

    return OptionResult.from(optionRepository.save(option));
  }

  @Transactional(readOnly = true)
  public List<OptionResult> list(UUID tenantId, UUID productId, UUID optionGroupId) {
    requireProduct(tenantId, productId);
    requireOptionGroup(tenantId, productId, optionGroupId);

    return optionRepository.findByTenantIdAndOptionGroupId(tenantId, optionGroupId).stream()
        .map(OptionResult::from)
        .toList();
  }

  @Transactional
  public OptionResult update(
      UUID tenantId,
      UUID productId,
      UUID optionGroupId,
      UUID optionId,
      UpdateOptionCommand command) {
    requireProduct(tenantId, productId);
    requireOptionGroup(tenantId, productId, optionGroupId);
    Option option = requireOption(tenantId, optionGroupId, optionId);
    option.updateDetails(command.name(), command.priceAdjustment());

    return OptionResult.from(optionRepository.save(option));
  }

  @Transactional
  public void changeStatus(
      UUID tenantId, UUID productId, UUID optionGroupId, UUID optionId, boolean active) {
    requireProduct(tenantId, productId);
    requireOptionGroup(tenantId, productId, optionGroupId);
    requireOption(tenantId, optionGroupId, optionId).changeStatusTo(active);
  }

  @Transactional
  public void reorder(
      UUID tenantId, UUID productId, UUID optionGroupId, ReorderOptionsCommand command) {
    requireProduct(tenantId, productId);
    requireOptionGroup(tenantId, productId, optionGroupId);

    List<Option> options = optionRepository.findByTenantIdAndOptionGroupId(tenantId, optionGroupId);
    List<UUID> requestedIds = command.optionIds();
    Set<UUID> uniqueRequestedIds = new HashSet<>(requestedIds);
    if (uniqueRequestedIds.size() != requestedIds.size()) {
      throw new IllegalArgumentException("optionIds must not contain duplicates");
    }

    Map<UUID, Option> optionsById =
        options.stream().collect(Collectors.toMap(Option::getId, Function.identity()));
    if (requestedIds.size() != options.size() || !optionsById.keySet().equals(uniqueRequestedIds)) {
      throw new IllegalArgumentException(
          "optionIds must contain exactly all option group option ids");
    }

    for (var position = 0; position < requestedIds.size(); position++) {
      optionsById.get(requestedIds.get(position)).changeDisplayPosition(position);
    }
  }

  private void requireProduct(UUID tenantId, UUID productId) {
    productRepository
        .findByIdAndTenantId(productId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_RESOURCE, productId));
  }

  private OptionGroup requireOptionGroup(UUID tenantId, UUID productId, UUID optionGroupId) {
    return optionGroupRepository
        .findByIdAndTenantIdAndProductId(optionGroupId, tenantId, productId)
        .orElseThrow(() -> new ResourceNotFoundException(OPTION_GROUP_RESOURCE, optionGroupId));
  }

  private Option requireOption(UUID tenantId, UUID optionGroupId, UUID optionId) {
    return optionRepository
        .findByIdAndTenantIdAndOptionGroupId(optionId, tenantId, optionGroupId)
        .orElseThrow(() -> new ResourceNotFoundException(OPTION_RESOURCE, optionId));
  }
}
