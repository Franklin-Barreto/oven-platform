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

class OptionTest {

  private static final UUID OPTION_GROUP_ID = UUID.randomUUID();
  private static final String VALID_NAME = "Brioche";
  private static final BigDecimal PRICE_ADJUSTMENT = new BigDecimal("3.00");
  private static final int DISPLAY_POSITION = 1;

  @Test
  void shouldCreateActiveOptionWithValidData() {
    var option = option(PRICE_ADJUSTMENT);

    assertThat(option.getOptionGroupId()).isEqualTo(OPTION_GROUP_ID);
    assertThat(option.getName()).isEqualTo(VALID_NAME);
    assertThat(option.getPriceAdjustment()).isEqualByComparingTo(PRICE_ADJUSTMENT);
    assertThat(option.getDisplayPosition()).isEqualTo(DISPLAY_POSITION);
    assertThat(option.isActive()).isTrue();
  }

  @Test
  void shouldAllowZeroPriceAdjustment() {
    assertThat(option(BigDecimal.ZERO).getPriceAdjustment()).isZero();
  }

  @Test
  void shouldTrimName() {
    var option = new Option(OPTION_GROUP_ID, "  " + VALID_NAME + "  ", PRICE_ADJUSTMENT, 0);

    assertThat(option.getName()).isEqualTo(VALID_NAME);
  }

  @ParameterizedTest
  @MethodSource("missingRequiredIdentifiers")
  void shouldRejectMissingRequiredIdentifier(UUID optionGroupId, String expectedMessage) {
    assertThatThrownBy(
            () -> new Option(optionGroupId, VALID_NAME, PRICE_ADJUSTMENT, DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  void shouldRejectInvalidName(String name, String expectedMessage) {
    assertThatThrownBy(() -> new Option(OPTION_GROUP_ID, name, PRICE_ADJUSTMENT, DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldRejectNullPriceAdjustment() {
    assertThatThrownBy(() -> new Option(OPTION_GROUP_ID, VALID_NAME, null, DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("priceAdjustment must not be null");
  }

  @Test
  void shouldRejectNegativePriceAdjustment() {
    assertThatThrownBy(
            () ->
                new Option(OPTION_GROUP_ID, VALID_NAME, new BigDecimal("-0.01"), DISPLAY_POSITION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("priceAdjustment must not be negative");
  }

  @Test
  void shouldRejectNegativeDisplayPosition() {
    assertThatThrownBy(() -> new Option(OPTION_GROUP_ID, VALID_NAME, PRICE_ADJUSTMENT, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("displayPosition must not be negative");
  }

  @Test
  void shouldUpdateDetailsWithoutChangingStatusOrPosition() {
    var option = option(BigDecimal.ZERO);
    option.changeStatusTo(false);

    option.updateDetails("Australiano", PRICE_ADJUSTMENT);

    assertThat(option.getName()).isEqualTo("Australiano");
    assertThat(option.getPriceAdjustment()).isEqualByComparingTo(PRICE_ADJUSTMENT);
    assertThat(option.getDisplayPosition()).isEqualTo(DISPLAY_POSITION);
    assertThat(option.isActive()).isFalse();
  }

  @Test
  void shouldKeepDetailsUnchangedWhenUpdateIsInvalid() {
    var option = option(PRICE_ADJUSTMENT);

    assertThatThrownBy(() -> option.updateDetails("Australiano", new BigDecimal("-0.01")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("priceAdjustment must not be negative");

    assertThat(option.getName()).isEqualTo(VALID_NAME);
    assertThat(option.getPriceAdjustment()).isEqualByComparingTo(PRICE_ADJUSTMENT);
  }

  @Test
  void shouldChangeDisplayPositionAndStatus() {
    var option = option(PRICE_ADJUSTMENT);

    option.changeDisplayPosition(0);
    option.changeStatusTo(false);

    assertThat(option.getDisplayPosition()).isZero();
    assertThat(option.isActive()).isFalse();
  }

  private static Stream<Arguments> missingRequiredIdentifiers() {
    return Stream.of(Arguments.of(null, "optionGroupId must not be null"));
  }

  private static Stream<Arguments> invalidNames() {
    return Stream.of(
        Arguments.of(null, "name must not be null"),
        Arguments.of("", "name must not be blank"),
        Arguments.of("   ", "name must not be blank"),
        Arguments.of("a".repeat(81), "name must have at most 80 characters"));
  }

  private static Option option(BigDecimal priceAdjustment) {
    return new Option(OPTION_GROUP_ID, VALID_NAME, priceAdjustment, DISPLAY_POSITION);
  }
}
