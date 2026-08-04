package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.CreateOptionGroupCommand;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupResult;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupService;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.ReorderOptionGroupsCommand;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaOptionGroupRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Import({
  OptionGroupService.class,
  JpaOptionGroupRepositoryAdapter.class,
  JpaProductRepositoryAdapter.class
})
class OptionGroupRepositoryIntegrationTest extends DataJpaIntegrationTest {

  @Autowired private OptionGroupRepository repository;
  @Autowired private OptionGroupService service;
  @Autowired private PlatformTransactionManager transactionManager;

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

    repository.save(additions);
    repository.save(bread);
    repository.save(removals);
    flushAndClear();

    assertThat(
            repository.findByTenantIdAndProductId(
                fixture.tenant().getId(), fixture.product().getId()))
        .extracting(OptionGroup::getName, OptionGroup::getDisplayPosition)
        .containsExactly(
            tuple("Escolha o pão", 0), tuple("Remover itens", 1), tuple("Adicionais", 2));
  }

  @Test
  void shouldRejectDuplicateDisplayPositionForTheSameProduct() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Torino");
    var sauces = optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Molhos", 1);
    var extras = optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Extras", 1);

    repository.save(sauces);
    repository.save(extras);
    entityManager.flush();

    assertThatThrownBy(this::enforceOptionGroupPositionConstraint)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("uk_option_groups_tenant_product_position");
  }

  @Test
  void shouldFindMaximumDisplayPosition() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Torino Max");
    repository.save(optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Molhos", 2));
    repository.save(optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Extras", 7));
    flushAndClear();

    assertThat(
            repository.findMaxDisplayPosition(fixture.tenant().getId(), fixture.product().getId()))
        .contains(7);
  }

  @Test
  void shouldReorderWithDeferredUniqueConstraint() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Reorder");
    var first =
        repository.save(
            optionGroup(fixture.product().getId(), fixture.tenant().getId(), "First", 0));
    var second =
        repository.save(
            optionGroup(fixture.product().getId(), fixture.tenant().getId(), "Second", 1));
    entityManager.flush();

    service.reorder(
        fixture.tenant().getId(),
        fixture.product().getId(),
        new ReorderOptionGroupsCommand(List.of(second.getId(), first.getId())));
    entityManager.flush();

    assertThat(first.getDisplayPosition()).isEqualTo(1);
    assertThat(second.getDisplayPosition()).isZero();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void shouldAssignDistinctPositionsToConcurrentCreates() throws Exception {
    var transaction = new TransactionTemplate(transactionManager);
    var ids =
        transaction.execute(
            ignored -> {
              var fixture = catalogFixture.createProductFixture("Pizzeria Concurrent");
              entityManager.flush();
              return new ProductIds(fixture.tenant().getId(), fixture.product().getId());
            });
    var start = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<OptionGroupResult> first =
          executor.submit(
              () -> {
                awaitStart(start);
                return service.create(
                    ids.tenantId(), ids.productId(), new CreateOptionGroupCommand("First", 0, 1));
              });
      Future<OptionGroupResult> second =
          executor.submit(
              () -> {
                awaitStart(start);
                return service.create(
                    ids.tenantId(), ids.productId(), new CreateOptionGroupCommand("Second", 0, 1));
              });

      start.countDown();

      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .extracting(OptionGroupResult::displayPosition)
          .containsExactlyInAnyOrder(0, 1);
    }
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

  private static void awaitStart(CountDownLatch start) throws InterruptedException {
    if (!start.await(5, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Timed out waiting for concurrent create start");
    }
  }

  private void enforceOptionGroupPositionConstraint() {
    entityManager
        .createNativeQuery("SET CONSTRAINTS uk_option_groups_tenant_product_position IMMEDIATE")
        .executeUpdate();
  }

  private record ProductIds(UUID tenantId, UUID productId) {}
}
