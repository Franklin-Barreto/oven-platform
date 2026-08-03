package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaOptionGroupRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import jakarta.persistence.PersistenceException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaOptionGroupRepositoryAdapter.class)
class OptionGroupRepositoryIntegrationTest extends DataJpaIntegrationTest {

  @Autowired private OptionGroupRepository repository;

  private CatalogTestFixture catalogFixture;

  @BeforeEach
  void setUp() {
    catalogFixture = new CatalogTestFixture(entityManager);
  }

  @Test
  void shouldSaveAndRetrieveOptionGroup() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Napoli");
    var optionGroup =
        optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Adicionais", 1);

    repository.save(optionGroup);
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                optionGroup.getId(), fixture.tenant().getId(), fixture.product().getId()))
        .hasValueSatisfying(
            persisted -> {
              assertThat(persisted.getProductId()).isEqualTo(fixture.product().getId());
              assertThat(persisted.getTenantId()).isEqualTo(fixture.tenant().getId());
              assertThat(persisted.getName()).isEqualTo("Adicionais");
              assertThat(persisted.getMinimumSelections()).isZero();
              assertThat(persisted.getMaximumSelections()).isEqualTo(5);
              assertThat(persisted.getDisplayPosition()).isEqualTo(1);
              assertThat(persisted.isActive()).isTrue();
            });
  }

  @Test
  void shouldReturnOptionGroupsInDisplayOrder() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Milano");
    var additions =
        optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Adicionais", 2);
    var bread =
        optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Escolha o pão", 0);
    var removals =
        optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Remover itens", 1);

    repository.saveAll(List.of(additions, bread, removals));
    flushAndClear();

    assertThat(
            repository.findByTenantIdAndProductId(
                fixture.tenant().getId(), fixture.product().getId()))
        .extracting(OptionGroup::getName, OptionGroup::getDisplayPosition)
        .containsExactly(
            tuple("Escolha o pão", 0), tuple("Remover itens", 1), tuple("Adicionais", 2));
  }

  @Test
  void shouldUseIdAsTieBreakerForOptionGroupsWithTheSameDisplayPosition() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Torino");
    var sauces = optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Molhos", 1);
    var extras = optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Extras", 1);

    repository.saveAll(List.of(sauces, extras));
    flushAndClear();

    var expectedIds =
        List.of(sauces.getId(), extras.getId()).stream()
            .sorted(Comparator.comparing(UUID::toString))
            .toList();

    assertThat(
            repository.findByTenantIdAndProductId(
                fixture.tenant().getId(), fixture.product().getId()))
        .extracting(OptionGroup::getId)
        .containsExactlyElementsOf(expectedIds);
  }

  @Test
  void shouldNotFindOptionGroupThroughAnotherProduct() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Bologna");
    var anotherProduct =
        catalogFixture.createProduct(
            fixture.tenant(), fixture.category(), fixture.image(), "Pizza Margherita");
    entityManager.flush();
    var optionGroup =
        repository.save(
            optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Molhos", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                optionGroup.getId(), fixture.tenant().getId(), anotherProduct.getId()))
        .isEmpty();
  }

  @Test
  void shouldNotFindOptionGroupThroughAnotherTenant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Firenze");
    var anotherFixture = catalogFixture.createProductFixture("Pizzeria Roma");
    var optionGroup =
        repository.save(
            optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Molhos", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                optionGroup.getId(), anotherFixture.tenant().getId(), fixture.product().getId()))
        .isEmpty();
    assertThat(
            repository.findByTenantIdAndProductId(
                anotherFixture.tenant().getId(), fixture.product().getId()))
        .isEmpty();
  }

  @Test
  void shouldRejectProductOwnedByAnotherTenant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Genova");
    var anotherFixture = catalogFixture.createProductFixture("Pizzeria Verona");
    repository.save(
        optionGroup(fixture.product().getId(), anotherFixture.tenant().getId(), "Molhos", 0));

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_option_groups_tenant_product");
  }

  private static OptionGroup optionGroup(UUID productId, UUID tenantId, String name, int position) {
    return new OptionGroup(productId, tenantId, name, 0, 5, position);
  }
}
