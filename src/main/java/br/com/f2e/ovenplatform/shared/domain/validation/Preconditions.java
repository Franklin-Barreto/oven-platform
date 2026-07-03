package br.com.f2e.ovenplatform.shared.domain.validation;

import java.math.BigDecimal;
import java.util.Collection;

public final class Preconditions {

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

  public static BigDecimal requirePositive(BigDecimal field, String fieldName) {
    requireNotNull(field, fieldName);

    if (field.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("%s must be greater than zero".formatted(fieldName));
    }

    return field;
  }

  public static int requirePositive(int field, String fieldName) {
    if (field <= 0) {
      throw new IllegalArgumentException("%s must be greater than zero".formatted(fieldName));
    }

    return field;
  }

  public static String requireMinimumSize(String field, String fieldName, int minimumSize) {
    var trimmed = requireNotBlank(field, fieldName);

    if (trimmed.length() < minimumSize) {
      throw new IllegalArgumentException(
          "%s must have at least %d characters".formatted(fieldName, minimumSize));
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
}
