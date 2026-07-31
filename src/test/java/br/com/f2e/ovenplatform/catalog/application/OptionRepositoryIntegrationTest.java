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
    var option = option(optionGroup.getId(), fixture.tenant().getId(), "Australiano", "3.00", 1);

    repository.save(option);
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndOptionGroupId(
                option.getId(), fixture.tenant().getId(), optionGroup.getId()))
        .hasValueSatisfying(
            persisted -> {
              assertThat(persisted.getTenantId()).isEqualTo(fixture.tenant().getId());
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
    var bacon = option(optionGroup.getId(), fixture.tenant().getId(), "Bacon", "5.00", 2);
    var cheese = option(optionGroup.getId(), fixture.tenant().getId(), "Queijo", "4.00", 0);
    var meat = option(optionGroup.getId(), fixture.tenant().getId(), "Carne", "10.00", 1);

    repository.saveAll(List.of(bacon, cheese, meat));
    flushAndClear();

    assertThat(
            repository.findByTenantIdAndOptionGroupId(
                fixture.tenant().getId(), optionGroup.getId()))
        .extracting(Option::getName, Option::getDisplayPosition)
        .containsExactly(tuple("Queijo", 0), tuple("Carne", 1), tuple("Bacon", 2));
  }

  @Test
  void shouldNotFindOptionThroughAnotherTenant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Firenze");
    var anotherFixture = catalogFixture.createProductFixture("Pizzeria Torino");
    var optionGroup = persistOptionGroup(fixture, "Molhos");
    var option =
        repository.save(
            option(optionGroup.getId(), fixture.tenant().getId(), "Ketchup", "0.00", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndOptionGroupId(
                option.getId(), anotherFixture.tenant().getId(), optionGroup.getId()))
        .isEmpty();
  }

  @Test
  void shouldNotFindOptionThroughAnotherOptionGroup() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Bologna");
    var optionGroup = persistOptionGroup(fixture, "Molhos");
    var anotherOptionGroup = persistOptionGroup(fixture, "Adicionais");
    var option =
        repository.save(
            option(optionGroup.getId(), fixture.tenant().getId(), "Ketchup", "0.00", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndOptionGroupId(
                option.getId(), fixture.tenant().getId(), anotherOptionGroup.getId()))
        .isEmpty();
  }

  @Test
  void shouldRejectOptionGroupFromAnotherTenant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Venezia");
    var anotherFixture = catalogFixture.createProductFixture("Pizzeria Genova");
    var anotherOptionGroup = persistOptionGroup(anotherFixture, "Molhos");
    repository.save(
        option(anotherOptionGroup.getId(), fixture.tenant().getId(), "Ketchup", "0.00", 0));

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_options_tenant_option_group");
  }

  @Test
  void shouldRejectNonexistentOptionGroup() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Verona");
    repository.save(option(UUID.randomUUID(), fixture.tenant().getId(), "Ketchup", "0.00", 0));

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_options_tenant_option_group");
  }

  private OptionGroup persistOptionGroup(CatalogTestFixture.ProductFixture fixture, String name) {
    var optionGroup =
        new OptionGroup(fixture.product().getId(), fixture.tenant().getId(), name, 0, 5, 0);
    entityManager.persist(optionGroup);
    entityManager.flush();
    return optionGroup;
  }

  private static Option option(
      UUID optionGroupId, UUID tenantId, String name, String priceAdjustment, int position) {
    return new Option(optionGroupId, tenantId, name, new BigDecimal(priceAdjustment), position);
  }
}
