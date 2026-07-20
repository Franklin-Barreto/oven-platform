package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static java.util.Objects.requireNonNull;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
class EventPublicationMetrics {

  private static final String INCOMPLETE_METRIC = "oven.events.publications.incomplete";
  private static final String FAILED_METRIC = "oven.events.publications.failed";

  private static final String OLDEST_INCOMPLETE_AGE_METRIC =
      "oven.events.publications.oldest.incomplete.age";

  private final Clock clock;
  private final AtomicReference<PublicationBacklogSnapshot> snapshot =
      new AtomicReference<>(PublicationBacklogSnapshot.empty());

  EventPublicationMetrics(MeterRegistry registry, Clock clock) {
    this.clock = requireNonNull(clock, "clock must not be null");
    requireNonNull(registry, "registry must not be null");

    Gauge.builder(INCOMPLETE_METRIC, snapshot, current -> current.get().incompleteCount())
        .description("Current number of incomplete durable event publications")
        .register(registry);

    Gauge.builder(FAILED_METRIC, snapshot, current -> current.get().failedCount())
        .description("Current number of failed durable event publications")
        .register(registry);

    Gauge.builder(
            OLDEST_INCOMPLETE_AGE_METRIC,
            snapshot,
            current -> oldestIncompleteAgeSeconds(current.get()))
        .description("Age of the oldest incomplete durable event publication")
        .baseUnit("seconds")
        .register(registry);
  }

  void update(PublicationBacklogSnapshot newSnapshot) {
    snapshot.set(requireNonNull(newSnapshot, "newSnapshot must not be null"));
  }

  private double oldestIncompleteAgeSeconds(PublicationBacklogSnapshot currentSnapshot) {

    return currentSnapshot.oldestPublicationDate().map(this::ageInSeconds).orElse(0.0);
  }

  private double ageInSeconds(java.time.Instant publicationDate) {
    var age = Duration.between(publicationDate, clock.instant());

    if (age.isNegative()) {
      return 0.0;
    }

    return age.toMillis() / 1_000.0;
  }
}
