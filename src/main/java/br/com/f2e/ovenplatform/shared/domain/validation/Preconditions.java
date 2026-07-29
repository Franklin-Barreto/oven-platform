package br.com.f2e.ovenplatform.shared.domain.validation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

public final class Preconditions {

  private static final String GREATER_THAN_ZERO = "%s must be greater than zero";

  private Preconditions() {}

  public static <T> T requireNotNull(T field, String fieldName) {
    if (field == null) {
      throw new IllegalArgumentException("%s must not be null".formatted(fieldName));
    }
    return field;
  }

  public static String requireNotBlank(String field, String fieldName) {
    requireNotNull(field, fieldName);

    var trimmed = field.trim();

    if (trimmed.isBlank()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }

    return trimmed;
  }

  public static String normalizeOptional(String field) {
    if (field == null || field.isBlank()) {
      return null;
    }

    return requireNotBlank(field, "field");
  }

  public static <T extends Number> T requirePositive(T value, String fieldName) {
    requireNotNull(value, fieldName);

    var numericValue = toBigDecimal(value, fieldName);

    if (numericValue.signum() <= 0) {
      throw new IllegalArgumentException(GREATER_THAN_ZERO.formatted(fieldName));
    }

    return value;
  }

  public static <T extends Number> T requireNonNegative(T value, String fieldName) {
    requireNotNull(value, fieldName);

    if (toBigDecimal(value, fieldName).signum() < 0) {
      throw new IllegalArgumentException("%s must not be negative".formatted(fieldName));
    }

    return value;
  }

  public static String requireMinimumSize(String field, String fieldName, int minimumSize) {
    var trimmed = requireNotBlank(field, fieldName);

    if (trimmed.length() < minimumSize) {
      throw new IllegalArgumentException(
          "%s must have at least %d characters".formatted(fieldName, minimumSize));
    }
    return trimmed;
  }

  public static String requireSize(
      String field, String fieldName, int minimumSize, int maximumSize) {
    var trimmed = requireMinimumSize(field, fieldName, minimumSize);

    if (trimmed.length() > maximumSize) {
      throw new IllegalArgumentException(
          "%s must have at most %d characters".formatted(fieldName, maximumSize));
    }

    return trimmed;
  }

  public static <T extends Collection<?>> T requireNotEmpty(T field, String fieldName) {
    requireNotNull(field, fieldName);

    if (field.isEmpty()) {
      throw new IllegalArgumentException("%s must have at least 1 item".formatted(fieldName));
    }

    return field;
  }

  public static <T extends Collection<?>> T requireNotEmptyAndWithoutNulls(
      T field, String fieldName) {
    requireNotEmpty(field, fieldName);

    if (field.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("%s must not contain null elements".formatted(fieldName));
    }

    return field;
  }

  private static BigDecimal toBigDecimal(Number value, String fieldName) {
    try {
      return new BigDecimal(value.toString());
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(
          "%s must be a finite number".formatted(fieldName), exception);
    }
  }
}
