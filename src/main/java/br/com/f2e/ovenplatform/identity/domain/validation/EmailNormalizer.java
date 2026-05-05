package br.com.f2e.ovenplatform.identity.domain.validation;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailNormalizer {
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  private EmailNormalizer() {}

  public static String normalize(String email) {
    var normalizedEmail = requireNotBlank(email, "email").toLowerCase(Locale.ROOT);

    if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
      throw new IllegalArgumentException("Invalid email format");
    }

    return normalizedEmail;
  }
}
