package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventPublicationMetricsTest {

  private static final String INCOMPLETE = "oven.events.publications.incomplete";
  private static final String FAILED = "oven.events.publications.failed";
  private static final String OLDEST_AGE = "oven.events.publications.oldest.incomplete.age";
  private static final Instant NOW = Instant.parse("2026-07-20T10:10:00Z");

  private SimpleMeterRegistry registry;
  private EventPublicationMetrics metrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    metrics = new EventPublicationMetrics(registry, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldExposeZeroValuesForEmptyBacklog() {
    assertThat(gaugeValue(INCOMPLETE)).isZero();
    assertThat(gaugeValue(FAILED)).isZero();
    assertThat(gaugeValue(OLDEST_AGE)).isZero();
  }

  @Test
  void shouldExposeCurrentBacklogFailedCountAndOldestAge() {
    metrics.update(
        PublicationBacklogSnapshot.withBacklog(3, 1, Instant.parse("2026-07-20T10:08:30Z")));

    assertThat(gaugeValue(INCOMPLETE)).isEqualTo(3);
    assertThat(gaugeValue(FAILED)).isEqualTo(1);
    assertThat(gaugeValue(OLDEST_AGE)).isEqualTo(90);
  }

  @Test
  void shouldExposeZeroAgeWhenPublicationDateIsInTheFuture() {
    metrics.update(
        PublicationBacklogSnapshot.withBacklog(1, 0, Instant.parse("2026-07-20T10:11:00Z")));

    assertThat(gaugeValue(OLDEST_AGE)).isZero();
  }

  private double gaugeValue(String name) {
    return registry.get(name).gauge().value();
  }
}
