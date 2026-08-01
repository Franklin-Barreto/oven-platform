package br.com.f2e.ovenplatform.catalog.application;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.f2e.ovenplatform.catalog.application.option.OptionRepository;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.application.product.CatalogProductOptionLookupService;
import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.Option;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogProductOptionLookupServiceTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("22b2759d-35b2-4b04-ab39-df2a203a652c");
  private static final UUID GROUP_ID = UUID.fromString("431e5f0b-a762-4d31-846f-e4fe83db6b19");
  private static final UUID OPTION_ID = UUID.fromString("3e9cba8e-6d8d-4b41-8c89-4f5fe8045bca");

  @Mock private ProductRepository productRepository;
  @Mock private OptionGroupRepository optionGroupRepository;
  @Mock private OptionRepository optionRepository;

  private CatalogProductOptionLookupService service;

  @BeforeEach
  void setUp() {
    service =
        new CatalogProductOptionLookupService(
            productRepository, optionGroupRepository, optionRepository);
  }

  @Test
  void shouldReturnEmptyWithoutQueryingOptionsWhenProductIsNotActive() {
    when(productRepository.findActiveByTenantIdAndIdIn(TENANT_ID, Set.of(PRODUCT_ID)))
        .thenReturn(List.of());

    assertThat(service.findActiveProductOptionConfiguration(TENANT_ID, PRODUCT_ID)).isEmpty();

    verify(optionGroupRepository, never()).findByTenantIdAndProductId(TENANT_ID, PRODUCT_ID);
    verify(optionRepository, never()).findByTenantIdAndOptionGroupId(TENANT_ID, GROUP_ID);
  }

  @Test
  void shouldReturnOnlyActiveGroupsAndOptions() {
    var activeGroup = optionGroup("Extras", 1, 2, GROUP_ID, true);
    var inactiveGroup = optionGroup("Hidden", 0, 1, UUID.randomUUID(), false);
    var activeOption = option("Cheese", new BigDecimal("2.50"), OPTION_ID, true);
    var inactiveOption = option("Unavailable", BigDecimal.ONE, UUID.randomUUID(), false);
    when(productRepository.findActiveByTenantIdAndIdIn(TENANT_ID, Set.of(PRODUCT_ID)))
        .thenReturn(List.of(mock(Product.class)));
    when(optionGroupRepository.findByTenantIdAndProductId(TENANT_ID, PRODUCT_ID))
        .thenReturn(List.of(activeGroup, inactiveGroup));
    when(optionRepository.findByTenantIdAndOptionGroupId(TENANT_ID, GROUP_ID))
        .thenReturn(List.of(activeOption, inactiveOption));

    var configuration =
        service.findActiveProductOptionConfiguration(TENANT_ID, PRODUCT_ID).orElseThrow();

    assertThat(configuration.productId()).isEqualTo(PRODUCT_ID);
    assertThat(configuration.optionGroups())
        .singleElement()
        .satisfies(
            group -> {
              assertThat(group.id()).isEqualTo(GROUP_ID);
              assertThat(group.name()).isEqualTo("Extras");
              assertThat(group.minimumSelections()).isEqualTo(1);
              assertThat(group.maximumSelections()).isEqualTo(2);
              assertThat(group.options())
                  .singleElement()
                  .satisfies(
                      option -> {
                        assertThat(option.id()).isEqualTo(OPTION_ID);
                        assertThat(option.name()).isEqualTo("Cheese");
                        assertThat(option.priceAdjustment()).isEqualByComparingTo("2.50");
                      });
            });
    verify(optionRepository, never())
        .findByTenantIdAndOptionGroupId(TENANT_ID, inactiveGroup.getId());
  }

  @Test
  void shouldExposeImmutableOptionCollections() {
    var groups =
        new ArrayList<br.com.f2e.ovenplatform.catalog.application.api.ActiveProductOptionGroup>();
    var configuration =
        new br.com.f2e.ovenplatform.catalog.application.api.ActiveProductOptionConfiguration(
            PRODUCT_ID, groups);
    groups.add(
        new br.com.f2e.ovenplatform.catalog.application.api.ActiveProductOptionGroup(
            GROUP_ID, "Extras", 0, 1, List.of()));
    var optionGroups = configuration.optionGroups();
    var firstGroup = groups.getFirst();
    var options = firstGroup.options();
    var option =
        new br.com.f2e.ovenplatform.catalog.application.api.ActiveProductOption(
            OPTION_ID, "Cheese", BigDecimal.ONE);

    assertThat(optionGroups).isEmpty();
    assertThatThrownBy(() -> optionGroups.add(firstGroup))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> options.add(option)).isInstanceOf(UnsupportedOperationException.class);
  }

  private static OptionGroup optionGroup(
      String name, int minimum, int maximum, UUID id, boolean active) {
    var group = withId(new OptionGroup(PRODUCT_ID, TENANT_ID, name, minimum, maximum, 0), id);
    group.changeStatusTo(active);
    return group;
  }

  private static Option option(String name, BigDecimal price, UUID id, boolean active) {
    var option = withId(new Option(GROUP_ID, TENANT_ID, name, price, 0), id);
    option.changeStatusTo(active);
    return option;
  }
}
