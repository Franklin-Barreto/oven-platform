package br.com.f2e.ovenplatform.media.infrastructure.cleanup;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oven.media.cleanup")
public record PendingImageCleanupProperties(
    Duration initialDelay, Duration fixedDelay, Duration pendingRetention) {

  public PendingImageCleanupProperties {
    requireNonNegative(initialDelay);
    requirePositive(fixedDelay, "fixedDelay");
    requirePositive(pendingRetention, "pendingRetention");
  }

  private static void requireNonNegative(Duration value) {
    if (value == null) {
      throw new IllegalArgumentException("initialDelay" + " must not be null");
    }

    if (value.isNegative()) {
      throw new IllegalArgumentException("initialDelay" + " must not be negative");
    }
  }

  private static void requirePositive(Duration value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " must not be null");
    }

    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
  }
}
