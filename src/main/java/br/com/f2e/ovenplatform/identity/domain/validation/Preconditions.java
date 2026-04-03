package br.com.f2e.ovenplatform.identity.domain.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Preconditions {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  private Preconditions() {}

  public static void requireNotNull(Object field, String fieldName) {
    if (field == null) {
      throw new IllegalArgumentException("%s must not be null".formatted(fieldName));
    }
  }

  public static void requireNotBlank(String field, String fieldName) {
    requireNotNull(field, fieldName);
    if (field.isBlank()) {
      throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
    }
  }

  public static String normalizeEmail(String email) {
    requireNotBlank(email, "email");
    var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
      throw new IllegalArgumentException("Invalid email format");
    }
    return normalizedEmail;
  }
}
