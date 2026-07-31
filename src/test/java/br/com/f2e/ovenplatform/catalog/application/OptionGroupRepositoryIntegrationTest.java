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
    var optionGroup = optionGroup(fixture.product().getId(), "Adicionais", 1);

    repository.save(optionGroup);
    flushAndClear();

    assertThat(repository.findByIdAndProductId(optionGroup.getId(), fixture.product().getId()))
        .hasValueSatisfying(
            persisted -> {
              assertThat(persisted.getProductId()).isEqualTo(fixture.product().getId());
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
    var additions = optionGroup(fixture.product().getId(), "Adicionais", 2);
    var bread = optionGroup(fixture.product().getId(), "Escolha o pão", 0);
    var removals = optionGroup(fixture.product().getId(), "Remover itens", 1);

    repository.saveAll(List.of(additions, bread, removals));
    flushAndClear();

    assertThat(repository.findByProductId(fixture.product().getId()))
        .extracting(OptionGroup::getName, OptionGroup::getDisplayPosition)
        .containsExactly(
            tuple("Escolha o pão", 0), tuple("Remover itens", 1), tuple("Adicionais", 2));
  }

  @Test
  void shouldNotFindOptionGroupThroughAnotherProduct() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Bologna");
    var anotherProduct =
        catalogFixture.createProduct(
            fixture.tenant(), fixture.category(), fixture.image(), "Pizza Margherita");
    entityManager.flush();
    var optionGroup = repository.save(optionGroup(fixture.product().getId(), "Molhos", 0));
    flushAndClear();

    assertThat(repository.findByIdAndProductId(optionGroup.getId(), anotherProduct.getId()))
        .isEmpty();
  }

  @Test
  void shouldRejectNonexistentProduct() {
    repository.save(optionGroup(UUID.randomUUID(), "Molhos", 0));

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_option_groups_product");
  }

  private static OptionGroup optionGroup(UUID productId, String name, int position) {
    return new OptionGroup(productId, name, 0, 5, position);
  }
}
