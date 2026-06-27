package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({CategoryService.class, JpaCategoryRepositoryAdapter.class})
@EnableJpaAuditing
class CategoryServiceIntegrationTest {

  private static final String VALID_NAME = "Pizzas";

  @Autowired private CategoryService categoryService;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private SpringDataTenantRepository tenantRepository;

  @Test
  void shouldCreateCategory() {
    var tenant = createTenant();

    var category = createCategory(tenant);

    assertThat(category)
        .satisfies(
            cat -> {
              assertThat(cat.getId()).isNotNull();
              assertThat(cat.isActive()).isTrue();
              assertThat(cat.getTenantId()).isEqualTo(tenant.getId());
              assertThat(cat.getName()).isEqualTo(VALID_NAME);
            });
  }

  @Test
  void shouldFindCategoryByIdAndTenantId() {
    var tenant = createTenant();
    var category = createCategory(tenant);

    var foundCategory = categoryService.findCategory(tenant.getId(), category.getId());

    assertThat(foundCategory)
        .isPresent()
        .get()
        .satisfies(
            found -> {
              assertThat(found.getId()).isEqualTo(category.getId());
              assertThat(found.getTenantId()).isEqualTo(tenant.getId());
              assertThat(found.getName()).isEqualTo(VALID_NAME);
            });
  }

  @Test
  void shouldReturnEmptyWhenCategoryDoesNotExist() {
    var tenant = createTenant();

    var foundCategory = categoryService.findCategory(tenant.getId(), UUID.randomUUID());

    assertThat(foundCategory).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenCategoryBelongsToAnotherTenant() {
    var ownerTenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var category = createCategory(ownerTenant);

    var foundCategory = categoryService.findCategory(anotherTenant.getId(), category.getId());

    assertThat(foundCategory).isEmpty();
  }

  @Test
  void shouldListCategoriesByTenant() {
    var tenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var category = createCategory(tenant);
    var inactiveCategory = createCategory(tenant, "Drinks");
    inactiveCategory.deactivate();
    categoryRepository.save(inactiveCategory);
    createCategory(anotherTenant, "Desserts");

    var categories = categoryService.listCategories(tenant.getId());

    assertThat(categories)
        .extracting(Category::getId)
        .containsOnly(category.getId(), inactiveCategory.getId());
  }

  @Test
  void shouldUpdateCategoryNameAndStatus() {
    var tenant = createTenant();
    var category = createCategory(tenant);

    var updatedCategory = categoryService.update(tenant.getId(), category.getId(), "Drinks", false);

    assertThat(updatedCategory.getName()).isEqualTo("Drinks");
    assertThat(updatedCategory.isActive()).isFalse();
  }

  @Test
  void shouldThrowResourceNotFoundWhenUpdatingUnknownCategory() {
    var tenant = createTenant();
    var tenantId = tenant.getId();
    var categoryId = UUID.randomUUID();

    ThrowableAssert.ThrowingCallable updateUnknownCategory =
        () -> categoryService.update(tenantId, categoryId, "Drinks", true);

    assertThatThrownBy(updateUnknownCategory)
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Category id: %s not found".formatted(categoryId));
  }

  @Test
  void shouldDeactivateCategory() {
    var tenant = createTenant();
    var category = createCategory(tenant);

    categoryService.deactivate(tenant.getId(), category.getId());

    assertThat(categoryService.findCategory(tenant.getId(), category.getId()))
        .isPresent()
        .get()
        .extracting(Category::isActive)
        .isEqualTo(false);
  }

  @Test
  void shouldThrowResourceNotFoundWhenDeactivatingCategoryFromAnotherTenant() {
    var ownerTenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var category = createCategory(ownerTenant);
    var tenantId = anotherTenant.getId();
    var categoryId = category.getId();

    ThrowableAssert.ThrowingCallable throwingCallable =
        () -> categoryService.deactivate(tenantId, categoryId);

    assertThatThrownBy(throwingCallable)
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Category id: %s not found".formatted(categoryId));
  }

  private Tenant createTenant() {
    return createTenant("Don Corleone Pizzeria");
  }

  private Tenant createTenant(String name) {
    return tenantRepository.save(new Tenant(name, Plan.MVP));
  }

  private Category createCategory(Tenant tenant) {
    return createCategory(tenant, VALID_NAME);
  }

  private Category createCategory(Tenant tenant, String name) {
    return categoryService.save(tenant.getId(), name);
  }
}
