package br.com.f2e.ovenplatform.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID CATEGORY_ID = UUID.randomUUID();
  private static final String VALID_NAME = "Pizza Portuguesa";
  private static final String VALID_DESCRIPTION = "Pizza com queijo, presunto e ovos";
  private static final BigDecimal VALID_PRICE = new BigDecimal("35.40");

  @Test
  void shouldCreateActiveProductWithValidData() {
    var product = product();

    assertThat(product.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(product.getCategoryId()).isEqualTo(CATEGORY_ID);
    assertThat(product.getName()).isEqualTo(VALID_NAME);
    assertThat(product.getDescription()).isEqualTo(VALID_DESCRIPTION);
    assertThat(product.getPrice()).isEqualByComparingTo(VALID_PRICE);
    assertThat(product.isActive()).isTrue();
  }

  @Test
  void shouldTrimProductNameWhenCreatingProduct() {
    var product =
        new Product(
            TENANT_ID, CATEGORY_ID, "Pizza portuguesa      ", VALID_DESCRIPTION, VALID_PRICE);

    assertThat(product.getName()).isEqualTo("Pizza portuguesa");
  }

  @Test
  void shouldRejectNullTenantId() {
    assertThatThrownBy(
            () -> new Product(null, CATEGORY_ID, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tenantId must not be null");
  }

  @Test
  void shouldRejectNullCategoryId() {
    assertThatThrownBy(
            () -> new Product(TENANT_ID, null, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId must not be null");
  }

  @ParameterizedTest
  @MethodSource("invalidProductNames")
  void shouldRejectInvalidProductName(String name, String expectedMessage) {
    assertThatThrownBy(
            () -> new Product(TENANT_ID, CATEGORY_ID, name, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldRejectNullPrice() {
    assertThatThrownBy(
            () -> new Product(TENANT_ID, CATEGORY_ID, VALID_NAME, VALID_DESCRIPTION, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must not be null");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-1.00"})
  void shouldRejectInvalidPrice(String value) {
    var price = new BigDecimal(value);

    assertThatThrownBy(
            () -> new Product(TENANT_ID, CATEGORY_ID, VALID_NAME, VALID_DESCRIPTION, price))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must be greater than zero");
  }

  @Test
  void shouldChangeProductCategoryWhenValid() {
    var product = product();
    var newCategoryId = UUID.randomUUID();

    product.changeCategory(newCategoryId);

    assertThat(product.getCategoryId()).isEqualTo(newCategoryId);
  }

  @Test
  void shouldRejectNullProductCategoryWhenChangingCategory() {
    var product = product();

    assertThatThrownBy(() -> product.changeCategory(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId must not be null");
  }

  @Test
  void shouldRenameProductWhenNameIsValid() {
    var product = product();

    product.rename("Pizza calabresa");

    assertThat(product.getName()).isEqualTo("Pizza calabresa");
  }

  @Test
  void shouldTrimProductNameWhenRenamingProduct() {
    var product = product();

    product.rename("Pizza calabresa      ");

    assertThat(product.getName()).isEqualTo("Pizza calabresa");
  }

  @ParameterizedTest
  @MethodSource("invalidProductNames")
  void shouldRejectInvalidProductNameWhenRenaming(String name, String expectedMessage) {
    var product = product();

    assertThatThrownBy(() -> product.rename(name))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldChangeProductDescriptionWhenValid() {
    var product = product();

    product.changeDescription("  Massa fina com borda recheada  ");

    assertThat(product.getDescription()).isEqualTo("Massa fina com borda recheada");
  }

  @Test
  void shouldClearProductDescriptionWhenBlank() {
    var product = product();

    product.changeDescription("   ");

    assertThat(product.getDescription()).isNull();
  }

  @Test
  void shouldRejectDescriptionWithMoreThan500Characters() {
    var product = product();
    var description = "a".repeat(501);

    assertThatThrownBy(() -> product.changeDescription(description))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("description must have at most 500 characters");
  }

  @Test
  void shouldChangeProductPriceWhenValid() {
    var product = product();
    var newPrice = new BigDecimal("25.00");

    product.changePrice(newPrice);

    assertThat(product.getPrice()).isEqualByComparingTo(newPrice);
  }

  @Test
  void shouldRejectNullProductPriceWhenChangingPrice() {
    var product = product();

    assertThatThrownBy(() -> product.changePrice(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must not be null");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-1.00"})
  void shouldRejectInvalidProductPriceWhenChangingPrice(String value) {
    var product = product();
    var price = new BigDecimal(value);

    assertThatThrownBy(() -> product.changePrice(price))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must be greater than zero");
  }

  @Test
  void shouldDeactivateProduct() {
    var product = product();

    product.deactivate();

    assertThat(product.isActive()).isFalse();
  }

  @Test
  void shouldActivateProduct() {
    var product = product();
    product.deactivate();

    product.activate();

    assertThat(product.isActive()).isTrue();
  }

  private static Stream<Arguments> invalidProductNames() {
    return Stream.of(
        Arguments.of(null, "name must not be null"),
        Arguments.of("", "name must not be blank"),
        Arguments.of(" ", "name must not be blank"),
        Arguments.of("   ", "name must not be blank"),
        Arguments.of("coca", "name must have at least 5 characters"));
  }

  private static Product product() {
    return new Product(TENANT_ID, CATEGORY_ID, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE);
  }
}
