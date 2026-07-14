package br.com.f2e.ovenplatform.shared.infrastructure.event;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oven.events.publication.maintenance")
public record EventPublicationMaintenanceProperties(
    Duration fixedDelay,
    Duration retryMinAge,
    int retryBatchSize,
    int retryMaxInFlight,
    int retryMaxAttempts,
    Duration completedRetention) {

  public EventPublicationMaintenanceProperties {
    requirePositive(fixedDelay, "fixedDelay");
    requirePositive(retryMinAge, "retryMinAge");
    requirePositive(retryBatchSize, "retryBatchSize");
    requirePositive(retryMaxInFlight, "retryMaxInFlight");
    requirePositive(retryMaxAttempts, "retryMaxAttempts");
    requirePositive(completedRetention, "completedRetention");
  }

  private static void requirePositive(Duration value, String name) {
    requireNonNull(value, name + " must not be null");

    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }

  private static void requirePositive(int value, String name) {
    if (value < 1) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }
}
