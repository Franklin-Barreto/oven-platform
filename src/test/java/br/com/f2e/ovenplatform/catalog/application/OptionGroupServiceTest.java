package br.com.f2e.ovenplatform.catalog.application;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.CreateOptionGroupCommand;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupResult;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupService;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.ReorderOptionGroupsCommand;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.UpdateOptionGroupCommand;
import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionGroupServiceTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("22b2759d-35b2-4b04-ab39-df2a203a652c");
  private static final UUID OPTION_GROUP_ID =
      UUID.fromString("431e5f0b-a762-4d31-846f-e4fe83db6b19");

  @Mock private ProductRepository productRepository;
  @Mock private OptionGroupRepository optionGroupRepository;

  private OptionGroupService service;

  @BeforeEach
  void setUp() {
    service = new OptionGroupService(productRepository, optionGroupRepository);
    lenient()
        .when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
        .thenReturn(Optional.of(org.mockito.Mockito.mock(Product.class)));
  }

  @Test
  void shouldCreateOptionGroupAfterCurrentLastPosition() {
    var existing = optionGroup("Existing", 3, UUID.randomUUID());
    when(optionGroupRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of(existing));
    when(optionGroupRepository.save(any(OptionGroup.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), OPTION_GROUP_ID));

    var result =
        service.create(TENANT_ID, PRODUCT_ID, new CreateOptionGroupCommand("Extras", 1, 2));

    assertThat(result)
        .isEqualTo(new OptionGroupResult(OPTION_GROUP_ID, PRODUCT_ID, "Extras", 1, 2, true, 4));
    var saved = ArgumentCaptor.forClass(OptionGroup.class);
    verify(optionGroupRepository).save(saved.capture());
    assertThat(saved.getValue().getDisplayPosition()).isEqualTo(4);
  }

  @Test
  void shouldCreateFirstOptionGroupAtPositionZero() {
    when(optionGroupRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of());
    when(optionGroupRepository.save(any(OptionGroup.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), OPTION_GROUP_ID));

    assertThat(service.create(TENANT_ID, PRODUCT_ID, new CreateOptionGroupCommand("Extras", 0, 1)))
        .extracting(OptionGroupResult::displayPosition)
        .isEqualTo(0);
  }

  @Test
  void shouldListOptionGroups() {
    var first = optionGroup("Sauces", 0, OPTION_GROUP_ID);
    var second = optionGroup("Extras", 1, UUID.randomUUID());
    when(optionGroupRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of(first, second));

    assertThat(service.list(TENANT_ID, PRODUCT_ID))
        .extracting(OptionGroupResult::name, OptionGroupResult::displayPosition)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Sauces", 0),
            org.assertj.core.groups.Tuple.tuple("Extras", 1));
  }

  @Test
  void shouldUpdateOptionGroupDetails() {
    var optionGroup = optionGroup("Sauces", 0, OPTION_GROUP_ID);
    when(optionGroupRepository.findByIdAndProductId(OPTION_GROUP_ID, PRODUCT_ID))
        .thenReturn(Optional.of(optionGroup));
    when(optionGroupRepository.save(optionGroup)).thenReturn(optionGroup);

    var result =
        service.update(
            TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, new UpdateOptionGroupCommand("Extras", 1, 3));

    assertThat(result)
        .extracting(
            OptionGroupResult::name,
            OptionGroupResult::minimumSelections,
            OptionGroupResult::maximumSelections)
        .containsExactly("Extras", 1, 3);
    verify(optionGroupRepository).save(optionGroup);
  }

  @Test
  void shouldChangeOptionGroupStatus() {
    var optionGroup = optionGroup("Sauces", 0, OPTION_GROUP_ID);
    when(optionGroupRepository.findByIdAndProductId(OPTION_GROUP_ID, PRODUCT_ID))
        .thenReturn(Optional.of(optionGroup));

    service.changeStatus(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, false);

    assertThat(optionGroup.isActive()).isFalse();
  }

  @Test
  void shouldReorderAllOptionGroups() {
    var first = optionGroup("First", 0, OPTION_GROUP_ID);
    var secondId = UUID.randomUUID();
    var second = optionGroup("Second", 1, secondId);
    when(optionGroupRepository.findByProductId(PRODUCT_ID)).thenReturn(List.of(first, second));

    service.reorder(
        TENANT_ID, PRODUCT_ID, new ReorderOptionGroupsCommand(List.of(secondId, OPTION_GROUP_ID)));

    assertThat(first.getDisplayPosition()).isEqualTo(1);
    assertThat(second.getDisplayPosition()).isZero();
  }

  @Test
  void shouldRejectReorderWithDuplicateIds() {
    when(optionGroupRepository.findByProductId(PRODUCT_ID))
        .thenReturn(List.of(optionGroup("First", 0, OPTION_GROUP_ID)));

    assertThatThrownBy(
            () ->
                service.reorder(
                    TENANT_ID,
                    PRODUCT_ID,
                    new ReorderOptionGroupsCommand(List.of(OPTION_GROUP_ID, OPTION_GROUP_ID))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("optionGroupIds must not contain duplicates");
  }

  @Test
  void shouldRejectReorderThatDoesNotContainExactlyTheProductOptionGroups() {
    when(optionGroupRepository.findByProductId(PRODUCT_ID))
        .thenReturn(List.of(optionGroup("First", 0, OPTION_GROUP_ID)));

    assertThatThrownBy(
            () ->
                service.reorder(
                    TENANT_ID,
                    PRODUCT_ID,
                    new ReorderOptionGroupsCommand(List.of(UUID.randomUUID()))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("optionGroupIds must contain exactly all product option group ids");
  }

  @Test
  void shouldRejectActionsWhenProductDoesNotBelongToTenant() {
    var unknownProductId = UUID.randomUUID();

    assertThatThrownBy(() -> service.list(TENANT_ID, unknownProductId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(unknownProductId));
    verify(optionGroupRepository, never()).findByProductId(unknownProductId);
  }

  @Test
  void shouldRejectUpdateWhenOptionGroupDoesNotBelongToProduct() {
    when(optionGroupRepository.findByIdAndProductId(OPTION_GROUP_ID, PRODUCT_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.update(
                    TENANT_ID,
                    PRODUCT_ID,
                    OPTION_GROUP_ID,
                    new UpdateOptionGroupCommand("Extras", 0, 1)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("OptionGroup id: %s not found".formatted(OPTION_GROUP_ID));
  }

  @Test
  void shouldDefensivelyCopyReorderCommandIds() {
    var ids = new ArrayList<>(List.of(OPTION_GROUP_ID));
    var command = new ReorderOptionGroupsCommand(ids);
    ids.clear();

    assertThat(command.optionGroupIds()).containsExactly(OPTION_GROUP_ID);
    assertThatThrownBy(() -> command.optionGroupIds().add(UUID.randomUUID()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static OptionGroup optionGroup(String name, int position, UUID id) {
    return withId(new OptionGroup(PRODUCT_ID, name, 0, 3, position), id);
  }
}
