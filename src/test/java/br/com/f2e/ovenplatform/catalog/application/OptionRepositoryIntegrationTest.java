package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.f2e.ovenplatform.catalog.application.option.OptionRepository;
import br.com.f2e.ovenplatform.catalog.domain.Option;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaOptionRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaOptionRepositoryAdapter.class)
class OptionRepositoryIntegrationTest extends DataJpaIntegrationTest {

  @Autowired private OptionRepository repository;

  private CatalogTestFixture catalogFixture;

  @BeforeEach
  void setUp() {
    catalogFixture = new CatalogTestFixture(entityManager);
  }

  @Test
  void shouldSaveAndRetrieveOption() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Napoli");
    var optionGroup = persistOptionGroup(fixture, "Escolha o pão");
    var option = option(optionGroup.getId(), "Australiano", "3.00", 1);

    repository.save(option);
    flushAndClear();

    assertThat(repository.findByIdAndOptionGroupId(option.getId(), optionGroup.getId()))
        .hasValueSatisfying(
            persisted -> {
              assertThat(persisted.getOptionGroupId()).isEqualTo(optionGroup.getId());
              assertThat(persisted.getName()).isEqualTo("Australiano");
              assertThat(persisted.getPriceAdjustment()).isEqualByComparingTo("3.00");
              assertThat(persisted.getDisplayPosition()).isEqualTo(1);
              assertThat(persisted.isActive()).isTrue();
            });
  }

  @Test
  void shouldReturnOptionsInDisplayOrder() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Milano");
    var optionGroup = persistOptionGroup(fixture, "Adicionais");
    var bacon = option(optionGroup.getId(), "Bacon", "5.00", 2);
    var cheese = option(optionGroup.getId(), "Queijo", "4.00", 0);
    var meat = option(optionGroup.getId(), "Carne", "10.00", 1);

    repository.saveAll(List.of(bacon, cheese, meat));
    flushAndClear();

    assertThat(repository.findByOptionGroupId(optionGroup.getId()))
        .extracting(Option::getName, Option::getDisplayPosition)
        .containsExactly(tuple("Queijo", 0), tuple("Carne", 1), tuple("Bacon", 2));
  }

  @Test
  void shouldNotFindOptionThroughAnotherOptionGroup() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Bologna");
    var optionGroup = persistOptionGroup(fixture, "Molhos");
    var anotherOptionGroup = persistOptionGroup(fixture, "Adicionais");
    var option = repository.save(option(optionGroup.getId(), "Ketchup", "0.00", 0));
    flushAndClear();

    assertThat(repository.findByIdAndOptionGroupId(option.getId(), anotherOptionGroup.getId()))
        .isEmpty();
  }

  @Test
  void shouldRejectNonexistentOptionGroup() {
    repository.save(option(UUID.randomUUID(), "Ketchup", "0.00", 0));

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_options_option_group");
  }

  private OptionGroup persistOptionGroup(CatalogTestFixture.ProductFixture fixture, String name) {
    var optionGroup = new OptionGroup(fixture.product().getId(), name, 0, 5, 0);
    entityManager.persist(optionGroup);
    entityManager.flush();
    return optionGroup;
  }

  private static Option option(
      UUID optionGroupId, String name, String priceAdjustment, int position) {
    return new Option(optionGroupId, name, new BigDecimal(priceAdjustment), position);
  }
}
