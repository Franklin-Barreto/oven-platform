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

class ProductVariantTest {

  private static final UUID PRODUCT_ID = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID IMAGE_ID = UUID.randomUUID();
  private static final String VALID_NAME = "Grande";
  private static final BigDecimal VALID_PRICE = new BigDecimal("59.90");
  private static final int DISPLAY_POSITION = 1;

  @Test
  void shouldCreateActiveProductVariantWithValidData() {
    var variant = variant(IMAGE_ID);

    assertThat(variant.getProductId()).isEqualTo(PRODUCT_ID);
    assertThat(variant.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(variant.getImageId()).isEqualTo(IMAGE_ID);
    assertThat(variant.getName()).isEqualTo(VALID_NAME);
    assertThat(variant.getPrice()).isEqualByComparingTo(VALID_PRICE);
    assertThat(variant.isActive()).isTrue();
    assertThat(variant.getDisplayPosition()).isEqualTo(DISPLAY_POSITION);
  }

  @Test
  void shouldCreateProductVariantWithoutImage() {
    assertThat(variant(null).getImageId()).isNull();
  }

  @Test
  void shouldTrimProductVariantName() {
    var variant =
        new ProductVariant(
            PRODUCT_ID, TENANT_ID, IMAGE_ID, "  Grande  ", VALID_PRICE, DISPLAY_POSITION);

    assertThat(variant.getName()).isEqualTo(VALID_NAME);
  }

  @ParameterizedTest
  @MethodSource("requiredIdentifiers")
  void shouldRejectMissingRequiredIdentifier(
      UUID productId, UUID tenantId, String expectedMessage) {
    assertThatThrownBy(
            () ->
                new ProductVariant(
                    productId, tenantId, IMAGE_ID, VALID_NAME, VALID_PRICE, DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  void shouldRejectInvalidName(String name, String expectedMessage) {
    assertThatThrownBy(
            () ->
                new ProductVariant(
                    PRODUCT_ID, TENANT_ID, IMAGE_ID, name, VALID_PRICE, DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldRejectNullPrice() {
    assertThatThrownBy(
            () ->
                new ProductVariant(
                    PRODUCT_ID, TENANT_ID, IMAGE_ID, VALID_NAME, null, DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must not be null");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-0.01"})
  void shouldRejectNonPositivePrice(String value) {

    var price = new BigDecimal(value);

    assertThatThrownBy(
            () ->
                new ProductVariant(
                    PRODUCT_ID, TENANT_ID, IMAGE_ID, VALID_NAME, price, DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must be greater than zero");
  }

  @Test
  void shouldAcceptZeroDisplayPosition() {
    var variant = new ProductVariant(PRODUCT_ID, TENANT_ID, IMAGE_ID, VALID_NAME, VALID_PRICE, 0);

    assertThat(variant.getDisplayPosition()).isZero();
  }

  @Test
  void shouldRejectNegativeDisplayPosition() {
    assertThatThrownBy(
            () -> new ProductVariant(PRODUCT_ID, TENANT_ID, IMAGE_ID, VALID_NAME, VALID_PRICE, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("displayPosition must not be negative");
  }

  @Test
  void shouldUpdateProductVariantDetails() {
    var variant = variant(IMAGE_ID);
    var newImageId = UUID.randomUUID();
    var newPrice = new BigDecimal("64.90");

    variant.updateDetails(newImageId, "  Família  ", newPrice, false);

    assertThat(variant.getImageId()).isEqualTo(newImageId);
    assertThat(variant.getName()).isEqualTo("Família");
    assertThat(variant.getPrice()).isEqualByComparingTo(newPrice);
    assertThat(variant.isActive()).isFalse();
  }

  @Test
  void shouldRemoveProductVariantImage() {
    var variant = variant(IMAGE_ID);

    variant.updateDetails(null, VALID_NAME, VALID_PRICE, true);

    assertThat(variant.getImageId()).isNull();
  }

  @Test
  void shouldKeepProductVariantUnchangedWhenUpdatedDetailsAreInvalid() {
    var variant = variant(IMAGE_ID);
    var imageId = UUID.randomUUID();

    assertThatThrownBy(() -> variant.updateDetails(imageId, "Família", BigDecimal.ZERO, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("price must be greater than zero");

    assertThat(variant.getImageId()).isEqualTo(IMAGE_ID);
    assertThat(variant.getName()).isEqualTo(VALID_NAME);
    assertThat(variant.getPrice()).isEqualByComparingTo(VALID_PRICE);
    assertThat(variant.isActive()).isTrue();
  }

  @Test
  void shouldChangeDisplayPosition() {
    var variant = variant(IMAGE_ID);

    variant.changeDisplayPosition(0);

    assertThat(variant.getDisplayPosition()).isZero();
  }

  @Test
  void shouldKeepDisplayPositionWhenNewPositionIsInvalid() {
    var variant = variant(IMAGE_ID);

    assertThatThrownBy(() -> variant.changeDisplayPosition(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("displayPosition must not be negative");

    assertThat(variant.getDisplayPosition()).isEqualTo(DISPLAY_POSITION);
  }

  @Test
  void shouldDeactivateProductVariant() {
    var variant = variant(IMAGE_ID);

    variant.deactivate();

    assertThat(variant.isActive()).isFalse();
  }

  @Test
  void shouldActivateProductVariant() {
    var variant = variant(IMAGE_ID);
    variant.deactivate();

    variant.activate();

    assertThat(variant.isActive()).isTrue();
  }

  private static Stream<Arguments> requiredIdentifiers() {
    return Stream.of(
        Arguments.of(null, TENANT_ID, "productId must not be null"),
        Arguments.of(PRODUCT_ID, null, "tenantId must not be null"));
  }

  private static Stream<Arguments> invalidNames() {
    return Stream.of(
        Arguments.of(null, "name must not be null"),
        Arguments.of("", "name must not be blank"),
        Arguments.of("   ", "name must not be blank"),
        Arguments.of("a".repeat(81), "name must have at most 80 characters"));
  }

  private static ProductVariant variant(UUID imageId) {
    return new ProductVariant(
        PRODUCT_ID, TENANT_ID, imageId, VALID_NAME, VALID_PRICE, DISPLAY_POSITION);
  }
}
