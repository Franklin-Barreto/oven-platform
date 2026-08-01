package br.com.f2e.ovenplatform.catalog.application;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.f2e.ovenplatform.catalog.application.option.*;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.Option;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.math.BigDecimal;
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
class OptionServiceTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("22b2759d-35b2-4b04-ab39-df2a203a652c");
  private static final UUID OPTION_GROUP_ID =
      UUID.fromString("431e5f0b-a762-4d31-846f-e4fe83db6b19");
  private static final UUID OPTION_ID = UUID.fromString("3e9cba8e-6d8d-4b41-8c89-4f5fe8045bca");

  @Mock private ProductRepository productRepository;
  @Mock private OptionGroupRepository optionGroupRepository;
  @Mock private OptionRepository optionRepository;

  private OptionService service;

  @BeforeEach
  void setUp() {
    service = new OptionService(productRepository, optionGroupRepository, optionRepository);
    lenient()
        .when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
        .thenReturn(Optional.of(mock(Product.class)));
    lenient()
        .when(
            optionGroupRepository.findByIdAndTenantIdAndProductId(
                OPTION_GROUP_ID, TENANT_ID, PRODUCT_ID))
        .thenReturn(Optional.of(mock(OptionGroup.class)));
  }

  @Test
  void shouldCreateOptionAfterCurrentLastPosition() {
    when(optionRepository.findByTenantIdAndOptionGroupId(TENANT_ID, OPTION_GROUP_ID))
        .thenReturn(List.of(option("Sauce", new BigDecimal("1.00"), 3, UUID.randomUUID())));
    when(optionRepository.save(any(Option.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), OPTION_ID));

    var result =
        service.create(
            TENANT_ID,
            PRODUCT_ID,
            OPTION_GROUP_ID,
            new CreateOptionCommand("Cheese", new BigDecimal("2.50")));

    assertThat(result)
        .isEqualTo(
            new OptionResult(
                OPTION_ID, OPTION_GROUP_ID, TENANT_ID, "Cheese", new BigDecimal("2.50"), true, 4));
    var saved = ArgumentCaptor.forClass(Option.class);
    verify(optionRepository).save(saved.capture());
    assertThat(saved.getValue().getDisplayPosition()).isEqualTo(4);
  }

  @Test
  void shouldCreateFirstOptionAtPositionZero() {
    when(optionRepository.findByTenantIdAndOptionGroupId(TENANT_ID, OPTION_GROUP_ID))
        .thenReturn(List.of());
    when(optionRepository.save(any(Option.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), OPTION_ID));

    assertThat(
            service
                .create(
                    TENANT_ID,
                    PRODUCT_ID,
                    OPTION_GROUP_ID,
                    new CreateOptionCommand("Cheese", BigDecimal.ZERO))
                .displayPosition())
        .isZero();
  }

  @Test
  void shouldListOptions() {
    when(optionRepository.findByTenantIdAndOptionGroupId(TENANT_ID, OPTION_GROUP_ID))
        .thenReturn(
            List.of(
                option("Sauce", BigDecimal.ZERO, 0, OPTION_ID),
                option("Cheese", new BigDecimal("2.50"), 1, UUID.randomUUID())));

    assertThat(service.list(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID))
        .extracting(OptionResult::name, OptionResult::displayPosition)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Sauce", 0),
            org.assertj.core.groups.Tuple.tuple("Cheese", 1));
  }

  @Test
  void shouldUpdateOptionDetailsAndStatus() {
    var option = option("Sauce", BigDecimal.ZERO, 0, OPTION_ID);
    when(optionRepository.findByIdAndTenantIdAndOptionGroupId(
            OPTION_ID, TENANT_ID, OPTION_GROUP_ID))
        .thenReturn(Optional.of(option));
    when(optionRepository.save(option)).thenReturn(option);

    var result =
        service.update(
            TENANT_ID,
            PRODUCT_ID,
            OPTION_GROUP_ID,
            OPTION_ID,
            new UpdateOptionCommand("Cheese", new BigDecimal("2.50")));
    service.changeStatus(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, OPTION_ID, false);

    assertThat(result)
        .extracting(OptionResult::name, OptionResult::priceAdjustment)
        .containsExactly("Cheese", new BigDecimal("2.50"));
    assertThat(option.isActive()).isFalse();
    verify(optionRepository).save(option);
  }

  @Test
  void shouldReorderAllOptions() {
    var first = option("First", BigDecimal.ZERO, 0, OPTION_ID);
    var secondId = UUID.randomUUID();
    var second = option("Second", BigDecimal.ZERO, 1, secondId);
    when(optionRepository.findByTenantIdAndOptionGroupId(TENANT_ID, OPTION_GROUP_ID))
        .thenReturn(List.of(first, second));

    service.reorder(
        TENANT_ID,
        PRODUCT_ID,
        OPTION_GROUP_ID,
        new ReorderOptionsCommand(List.of(secondId, OPTION_ID)));

    assertThat(first.getDisplayPosition()).isEqualTo(1);
    assertThat(second.getDisplayPosition()).isZero();
  }

  @Test
  void shouldRejectInvalidReorders() {
    when(optionRepository.findByTenantIdAndOptionGroupId(TENANT_ID, OPTION_GROUP_ID))
        .thenReturn(List.of(option("First", BigDecimal.ZERO, 0, OPTION_ID)));
    var optionsCommand = new ReorderOptionsCommand(List.of(OPTION_ID, OPTION_ID));
    var reorderOptionsCommand = new ReorderOptionsCommand(List.of(UUID.randomUUID()));
    assertThatThrownBy(
            () -> service.reorder(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, optionsCommand))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("optionIds must not contain duplicates");
    assertThatThrownBy(
            () -> service.reorder(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, reorderOptionsCommand))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("optionIds must contain exactly all option group option ids");
  }

  @Test
  void shouldRejectMissingResourcesBeforeQueryingOptions() {
    var unknownProductId = UUID.randomUUID();
    assertThatThrownBy(() -> service.list(TENANT_ID, unknownProductId, OPTION_GROUP_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(optionRepository, never()).findByTenantIdAndOptionGroupId(TENANT_ID, OPTION_GROUP_ID);

    when(optionGroupRepository.findByIdAndTenantIdAndProductId(
            OPTION_GROUP_ID, TENANT_ID, PRODUCT_ID))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.list(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldRejectOptionOutsideOptionGroup() {
    when(optionRepository.findByIdAndTenantIdAndOptionGroupId(
            OPTION_ID, TENANT_ID, OPTION_GROUP_ID))
        .thenReturn(Optional.empty());
    var optionCommand = new UpdateOptionCommand("Cheese", BigDecimal.ZERO);

    assertThatThrownBy(
            () -> service.update(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, OPTION_ID, optionCommand))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Option id: %s not found".formatted(OPTION_ID));
  }

  private static Option option(String name, BigDecimal priceAdjustment, int position, UUID id) {
    return withId(new Option(OPTION_GROUP_ID, TENANT_ID, name, priceAdjustment, position), id);
  }
}
