package br.com.f2e.ovenplatform.catalog.application.optiongroup;

import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptionGroupService {

  private static final String PRODUCT_RESOURCE = "Product";
  private static final String OPTION_GROUP_RESOURCE = "OptionGroup";

  private final ProductRepository productRepository;
  private final OptionGroupRepository optionGroupRepository;

  public OptionGroupService(
      ProductRepository productRepository, OptionGroupRepository optionGroupRepository) {
    this.productRepository = productRepository;
    this.optionGroupRepository = optionGroupRepository;
  }

  @Transactional
  public OptionGroupResult create(UUID tenantId, UUID productId, CreateOptionGroupCommand command) {
    requireProductForUpdate(tenantId, productId);

    int nextDisplayPosition =
        optionGroupRepository.findMaxDisplayPosition(tenantId, productId).orElse(-1) + 1;
    OptionGroup optionGroup =
        new OptionGroup(
            productId,
            tenantId,
            command.name(),
            command.minimumSelections(),
            command.maximumSelections(),
            nextDisplayPosition);

    return OptionGroupResult.from(optionGroupRepository.save(optionGroup));
  }

  @Transactional(readOnly = true)
  public List<OptionGroupResult> list(UUID tenantId, UUID productId) {
    requireProduct(tenantId, productId);

    return optionGroupRepository.findByTenantIdAndProductId(tenantId, productId).stream()
        .map(OptionGroupResult::from)
        .toList();
  }

  @Transactional
  public OptionGroupResult update(
      UUID tenantId, UUID productId, UUID optionGroupId, UpdateOptionGroupCommand command) {
    requireProduct(tenantId, productId);
    OptionGroup optionGroup = requireOptionGroup(tenantId, productId, optionGroupId);
    optionGroup.updateDetails(
        command.name(), command.minimumSelections(), command.maximumSelections());

    return OptionGroupResult.from(optionGroupRepository.save(optionGroup));
  }

  @Transactional
  public void changeStatus(UUID tenantId, UUID productId, UUID optionGroupId, boolean active) {
    requireProduct(tenantId, productId);
    OptionGroup optionGroup = requireOptionGroup(tenantId, productId, optionGroupId);
    optionGroup.changeStatusTo(active);
  }

  @Transactional
  public void reorder(UUID tenantId, UUID productId, ReorderOptionGroupsCommand command) {
    requireProductForUpdate(tenantId, productId);

    List<OptionGroup> optionGroups =
        optionGroupRepository.findByTenantIdAndProductId(tenantId, productId);
    List<UUID> requestedIds = command.optionGroupIds();
    HashSet<UUID> uniqueRequestedIds = new HashSet<>(requestedIds);

    if (uniqueRequestedIds.size() != requestedIds.size()) {
      throw new IllegalArgumentException("optionGroupIds must not contain duplicates");
    }

    Map<UUID, OptionGroup> optionGroupsById =
        optionGroups.stream().collect(Collectors.toMap(OptionGroup::getId, Function.identity()));
    if (requestedIds.size() != optionGroups.size()
        || !optionGroupsById.keySet().equals(uniqueRequestedIds)) {
      throw new IllegalArgumentException(
          "optionGroupIds must contain exactly all product option group ids");
    }

    for (var position = 0; position < requestedIds.size(); position++) {
      optionGroupsById.get(requestedIds.get(position)).changeDisplayPosition(position);
    }
  }

  private void requireProduct(UUID tenantId, UUID productId) {
    productRepository
        .findByIdAndTenantId(productId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_RESOURCE, productId));
  }

  private void requireProductForUpdate(UUID tenantId, UUID productId) {
    productRepository
        .findByIdAndTenantIdForUpdate(productId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_RESOURCE, productId));
  }

  private OptionGroup requireOptionGroup(UUID tenantId, UUID productId, UUID optionGroupId) {
    return optionGroupRepository
        .findByIdAndTenantIdAndProductId(optionGroupId, tenantId, productId)
        .orElseThrow(() -> new ResourceNotFoundException(OPTION_GROUP_RESOURCE, optionGroupId));
  }
}
