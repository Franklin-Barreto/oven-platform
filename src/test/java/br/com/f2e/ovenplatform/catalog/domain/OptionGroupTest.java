package br.com.f2e.ovenplatform.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OptionGroupTest {

  private static final UUID PRODUCT_ID = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final String VALID_NAME = "Escolha o pão";
  private static final int MINIMUM_SELECTIONS = 1;
  private static final int MAXIMUM_SELECTIONS = 2;
  private static final int DISPLAY_POSITION = 1;

  @Test
  void shouldCreateActiveRequiredOptionGroupWithValidData() {
    var group = group();

    assertThat(group.getProductId()).isEqualTo(PRODUCT_ID);
    assertThat(group.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(group.getName()).isEqualTo(VALID_NAME);
    assertThat(group.getMinimumSelections()).isEqualTo(MINIMUM_SELECTIONS);
    assertThat(group.getMaximumSelections()).isEqualTo(MAXIMUM_SELECTIONS);
    assertThat(group.getDisplayPosition()).isEqualTo(DISPLAY_POSITION);
    assertThat(group.isActive()).isTrue();
    assertThat(group.isRequired()).isTrue();
  }

  @Test
  void shouldCreateOptionalOptionGroupWhenMinimumSelectionsIsZero() {
    var group = new OptionGroup(PRODUCT_ID, TENANT_ID, VALID_NAME, 0, 4, DISPLAY_POSITION);

    assertThat(group.isRequired()).isFalse();
  }

  @Test
  void shouldAllowZeroMaximumSelections() {
    var group = new OptionGroup(PRODUCT_ID, TENANT_ID, VALID_NAME, 0, 0, DISPLAY_POSITION);

    assertThat(group.getMaximumSelections()).isZero();
  }

  @Test
  void shouldTrimName() {
    var group =
        new OptionGroup(
            PRODUCT_ID,
            TENANT_ID,
            "  " + VALID_NAME + "  ",
            MINIMUM_SELECTIONS,
            MAXIMUM_SELECTIONS,
            DISPLAY_POSITION);

    assertThat(group.getName()).isEqualTo(VALID_NAME);
  }

  @ParameterizedTest
  @MethodSource("missingRequiredIdentifiers")
  void shouldRejectMissingRequiredIdentifier(
      UUID productId, UUID tenantId, String expectedMessage) {
    assertThatThrownBy(
            () ->
                new OptionGroup(
                    productId,
                    tenantId,
                    VALID_NAME,
                    MINIMUM_SELECTIONS,
                    MAXIMUM_SELECTIONS,
                    DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  void shouldRejectInvalidName(String name, String expectedMessage) {
    assertThatThrownBy(
            () ->
                new OptionGroup(
                    PRODUCT_ID,
                    TENANT_ID,
                    name,
                    MINIMUM_SELECTIONS,
                    MAXIMUM_SELECTIONS,
                    DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidSelectionLimits")
  void shouldRejectInvalidSelectionLimits(int minimum, int maximum, String expectedMessage) {
    assertThatThrownBy(
            () -> new OptionGroup(PRODUCT_ID, TENANT_ID, VALID_NAME, minimum, maximum, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldRejectNegativeDisplayPosition() {
    assertThatThrownBy(
            () ->
                new OptionGroup(
                    PRODUCT_ID, TENANT_ID, VALID_NAME, MINIMUM_SELECTIONS, MAXIMUM_SELECTIONS, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("displayPosition must not be negative");
  }

  @Test
  void shouldUpdateDetailsWithoutChangingStatusOrPosition() {
    var group = group();
    group.changeStatusTo(false);

    group.updateDetails("Remover ingredientes", 0, 4);

    assertThat(group.getName()).isEqualTo("Remover ingredientes");
    assertThat(group.getMinimumSelections()).isZero();
    assertThat(group.getMaximumSelections()).isEqualTo(4);
    assertThat(group.getDisplayPosition()).isEqualTo(DISPLAY_POSITION);
    assertThat(group.isActive()).isFalse();
    assertThat(group.isRequired()).isFalse();
  }

  @Test
  void shouldKeepDetailsUnchangedWhenUpdateSelectionLimitsAreInvalid() {
    var group = group();

    assertThatThrownBy(() -> group.updateDetails("Remover ingredientes", 4, 3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("maximumSelections must be greater than or equal to minimumSelections");

    assertThat(group.getName()).isEqualTo(VALID_NAME);
    assertThat(group.getMinimumSelections()).isEqualTo(MINIMUM_SELECTIONS);
    assertThat(group.getMaximumSelections()).isEqualTo(MAXIMUM_SELECTIONS);
  }

  @Test
  void shouldChangeDisplayPositionAndStatus() {
    var group = group();

    group.changeDisplayPosition(0);
    group.changeStatusTo(false);

    assertThat(group.getDisplayPosition()).isZero();
    assertThat(group.isActive()).isFalse();
  }

  private static Stream<Arguments> missingRequiredIdentifiers() {
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

  private static Stream<Arguments> invalidSelectionLimits() {
    return Stream.of(
        Arguments.of(-1, 0, "minimumSelections must not be negative"),
        Arguments.of(0, -1, "maximumSelections must not be negative"),
        Arguments.of(2, 1, "maximumSelections must be greater than or equal to minimumSelections"));
  }

  private static OptionGroup group() {
    return new OptionGroup(
        PRODUCT_ID,
        TENANT_ID,
        VALID_NAME,
        MINIMUM_SELECTIONS,
        MAXIMUM_SELECTIONS,
        DISPLAY_POSITION);
  }
}
