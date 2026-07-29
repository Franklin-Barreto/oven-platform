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
  private static final UUID IMAGE_ID = UUID.randomUUID();
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
            TENANT_ID,
            CATEGORY_ID,
            IMAGE_ID,
            "Pizza portuguesa      ",
            VALID_DESCRIPTION,
            VALID_PRICE);

    assertThat(product.getName()).isEqualTo("Pizza portuguesa");
  }

  @Test
  void shouldRejectNullTenantId() {
    assertThatThrownBy(
            () ->
                new Product(
                    null, CATEGORY_ID, IMAGE_ID, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tenantId must not be null");
  }

  @Test
  void shouldRejectNullCategoryId() {
    assertThatThrownBy(
            () ->
                new Product(TENANT_ID, null, IMAGE_ID, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId must not be null");
  }

  @Test
  void shouldRejectNullPrice() {
    assertThatThrownBy(
            () ->
                new Product(TENANT_ID, CATEGORY_ID, IMAGE_ID, VALID_NAME, VALID_DESCRIPTION, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must not be null");
  }

  @ParameterizedTest
  @MethodSource("invalidProductNames")
  void shouldRejectInvalidProductName(String name, String expectedMessage) {
    assertThatThrownBy(
            () ->
                new Product(TENANT_ID, CATEGORY_ID, IMAGE_ID, name, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-1.00"})
  void shouldRejectInvalidPrice(String value) {
    var price = new BigDecimal(value);

    assertThatThrownBy(
            () ->
                new Product(TENANT_ID, CATEGORY_ID, IMAGE_ID, VALID_NAME, VALID_DESCRIPTION, price))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must be greater than zero");
  }

  @Test
  void shouldUpdateProductDetails() {
    var product = product();
    var newCategoryId = UUID.randomUUID();
    var newImageId = UUID.randomUUID();
    var newPrice = new BigDecimal("25.00");

    product.updateDetails(
        newCategoryId,
        newImageId,
        "  Pizza calabresa  ",
        "  Massa fina com borda recheada  ",
        newPrice,
        false);

    assertThat(product.getCategoryId()).isEqualTo(newCategoryId);
    assertThat(product.getImageId()).isEqualTo(newImageId);
    assertThat(product.getName()).isEqualTo("Pizza calabresa");
    assertThat(product.getDescription()).isEqualTo("Massa fina com borda recheada");
    assertThat(product.getPrice()).isEqualByComparingTo(newPrice);
    assertThat(product.isActive()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("invalidProductNames")
  void shouldRejectInvalidProductNameWhenUpdating(String name, String expectedMessage) {
    var product = product();

    assertThatThrownBy(
            () ->
                product.updateDetails(
                    CATEGORY_ID, IMAGE_ID, name, VALID_DESCRIPTION, VALID_PRICE, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldClearProductDescriptionWhenUpdatingWithBlankValue() {
    var product = product();

    product.updateDetails(CATEGORY_ID, IMAGE_ID, VALID_NAME, "   ", VALID_PRICE, true);

    assertThat(product.getDescription()).isNull();
  }

  @Test
  void shouldRejectDescriptionWithMoreThan500Characters() {
    var product = product();
    var description = "a".repeat(501);

    assertThatThrownBy(
            () ->
                product.updateDetails(
                    CATEGORY_ID, IMAGE_ID, VALID_NAME, description, VALID_PRICE, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("description must have at most 500 characters");
  }

  @Test
  void shouldRejectNullProductCategoryWhenUpdating() {
    var product = product();

    assertThatThrownBy(
            () ->
                product.updateDetails(
                    null, IMAGE_ID, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId must not be null");
  }

  @Test
  void shouldRejectNullProductImageWhenUpdating() {
    var product = product();

    assertThatThrownBy(
            () ->
                product.updateDetails(
                    CATEGORY_ID, null, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("imageId must not be null");
  }

  @Test
  void shouldKeepProductUnchangedWhenUpdatedDetailsAreInvalid() {
    var product = product();
    var originalCategoryId = product.getCategoryId();
    var originalImageId = product.getImageId();
    var originalName = product.getName();
    var originalDescription = product.getDescription();
    var originalPrice = product.getPrice();
    var originalActive = product.isActive();
    var categoryId = UUID.randomUUID();
    var imageId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                product.updateDetails(
                    categoryId,
                    imageId,
                    "Pizza calabresa",
                    "Nova descrição",
                    BigDecimal.ZERO,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must be greater than zero");

    assertThat(product.getCategoryId()).isEqualTo(originalCategoryId);
    assertThat(product.getImageId()).isEqualTo(originalImageId);
    assertThat(product.getName()).isEqualTo(originalName);
    assertThat(product.getDescription()).isEqualTo(originalDescription);
    assertThat(product.getPrice()).isEqualByComparingTo(originalPrice);
    assertThat(product.isActive()).isEqualTo(originalActive);
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

  @Test
  void shouldCreateProductWithImage() {
    assertThat(product().getImageId()).isEqualTo(IMAGE_ID);
  }

  @Test
  void shouldRejectNullImageWhenCreatingProduct() {
    assertThatThrownBy(
            () ->
                new Product(
                    TENANT_ID, CATEGORY_ID, null, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("imageId must not be null");
  }

  private static Stream<Arguments> invalidProductNames() {
    return Stream.of(
        Arguments.of(null, "name must not be null"),
        Arguments.of("", "name must not be blank"),
        Arguments.of(" ", "name must not be blank"),
        Arguments.of("   ", "name must not be blank"),
        Arguments.of("coca", "name must have at least 5 characters"),
        Arguments.of("a".repeat(81), "name must have at most 80 characters"));
  }

  private static Product product() {
    return new Product(
        TENANT_ID, CATEGORY_ID, IMAGE_ID, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE);
  }
}
