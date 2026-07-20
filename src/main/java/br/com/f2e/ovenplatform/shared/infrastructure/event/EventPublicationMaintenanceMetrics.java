package br.com.f2e.ovenplatform.shared.infrastructure.event;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class EventPublicationMaintenanceMetrics {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(EventPublicationMaintenanceMetrics.class);

  private static final String RESUBMISSIONS = "oven.events.publications.resubmissions";
  private static final String CLEANUP = "oven.events.publications.cleanup";

  private final Counter successfulResubmissions;
  private final Counter failedResubmissions;
  private final Counter successfulCleanup;
  private final Counter failedCleanup;

  EventPublicationMaintenanceMetrics(MeterRegistry registry) {
    successfulResubmissions = counter(registry, RESUBMISSIONS, "success");
    failedResubmissions = counter(registry, RESUBMISSIONS, "failure");
    successfulCleanup = counter(registry, CLEANUP, "success");
    failedCleanup = counter(registry, CLEANUP, "failure");
  }

  void recordResubmissionSuccess() {
    incrementSafely(successfulResubmissions);
  }

  void recordResubmissionFailure() {
    incrementSafely(failedResubmissions);
  }

  void recordCleanupSuccess() {
    incrementSafely(successfulCleanup);
  }

  void recordCleanupFailure() {
    incrementSafely(failedCleanup);
  }

  private static Counter counter(MeterRegistry registry, String name, String result) {
    return Counter.builder(name)
        .description("Number of durable event publication maintenance executions")
        .tag("result", result)
        .register(registry);
  }

  private static void incrementSafely(Counter counter) {
    try {
      counter.increment();
    } catch (RuntimeException exception) {
      LOGGER.error("Failed to record event publication maintenance metric", exception);
    }
  }
}
