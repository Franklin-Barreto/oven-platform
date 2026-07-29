package br.com.f2e.ovenplatform.shared.domain.validation;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNonNegative;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PreconditionsTest {

  @ParameterizedTest
  @MethodSource("supportedPositiveNumbers")
  void shouldAcceptSupportedPositiveNumberTypes(Number value) {
    assertThat(requirePositive(value, "value")).isEqualTo(value);
  }

  @ParameterizedTest
  @MethodSource("nonPositiveNumbers")
  void shouldRejectNonPositiveNumbers(Number value) {
    assertThatThrownBy(() -> requirePositive(value, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must be greater than zero");
  }

  @ParameterizedTest
  @MethodSource("supportedZeros")
  void shouldAcceptZeroAsNonNegative(Number value) {
    assertThat(requireNonNegative(value, "value")).isEqualTo(value);
  }

  @ParameterizedTest
  @MethodSource("negativeNumbers")
  void shouldRejectNegativeNumbers(Number value) {
    assertThatThrownBy(() -> requireNonNegative(value, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be negative");
  }

  private static Stream<Arguments> supportedPositiveNumbers() {
    return Stream.of(Arguments.of(1), Arguments.of(1L), Arguments.of(new BigDecimal("0.01")));
  }

  private static Stream<Arguments> nonPositiveNumbers() {
    return Stream.of(
        Arguments.of(0),
        Arguments.of(0L),
        Arguments.of(BigDecimal.ZERO),
        Arguments.of(-1),
        Arguments.of(-1L),
        Arguments.of(new BigDecimal("-0.01")));
  }

  private static Stream<Arguments> supportedZeros() {
    return Stream.of(Arguments.of(0), Arguments.of(0L), Arguments.of(BigDecimal.ZERO));
  }

  private static Stream<Arguments> negativeNumbers() {
    return Stream.of(Arguments.of(-1), Arguments.of(-1L), Arguments.of(new BigDecimal("-0.01")));
  }
}
