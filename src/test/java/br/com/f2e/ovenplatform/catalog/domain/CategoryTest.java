package br.com.f2e.ovenplatform.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CategoryTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final String VALID_NAME = "Pizzas";

  @Test
  void shouldCreateActiveCategoryWithValidData() {
    var category = category();

    assertThat(category.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(category.getName()).isEqualTo(VALID_NAME);
    assertThat(category.isActive()).isTrue();
  }

  @Test
  void shouldTrimCategoryNameWhenCreatingCategory() {
    var category = new Category("Pizzas      ", TENANT_ID);

    assertThat(category.getName()).isEqualTo("Pizzas");
  }

  @Test
  void shouldRejectNullTenantId() {
    assertThatThrownBy(() -> new Category(VALID_NAME, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tenantId must not be null");
  }

  @ParameterizedTest
  @MethodSource("invalidCategoryNames")
  void shouldRejectInvalidCategoryName(String name, String expectedMessage) {
    assertThatThrownBy(() -> new Category(name, TENANT_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldRenameCategoryWhenNameIsValid() {
    var category = category();

    category.rename("Drinks");

    assertThat(category.getName()).isEqualTo("Drinks");
  }

  @Test
  void shouldTrimCategoryNameWhenRenamingCategory() {
    var category = category();

    category.rename("Drinks      ");

    assertThat(category.getName()).isEqualTo("Drinks");
  }

  @ParameterizedTest
  @MethodSource("invalidCategoryNames")
  void shouldRejectInvalidCategoryNameWhenRenaming(String name, String expectedMessage) {
    var category = category();

    assertThatThrownBy(() -> category.rename(name))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldDeactivateCategory() {
    var category = category();

    category.deactivate();

    assertThat(category.isActive()).isFalse();
  }

  @Test
  void shouldActivateCategory() {
    var category = category();
    category.deactivate();

    category.activate();

    assertThat(category.isActive()).isTrue();
  }

  private static Stream<Arguments> invalidCategoryNames() {
    return Stream.of(
        Arguments.of(null, "name must not be null"),
        Arguments.of("", "name must not be blank"),
        Arguments.of(" ", "name must not be blank"),
        Arguments.of("   ", "name must not be blank"),
        Arguments.of("beer", "name must have at least 5 characters"));
  }

  private static Category category() {
    return new Category(VALID_NAME, TENANT_ID);
  }
}
