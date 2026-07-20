package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  PostgresTestContainerConfiguration.class,
  JdbcPublicationBacklogReader.class,
  EventPublicationMetrics.class,
  EventPublicationBacklogMonitor.class,
  EventPublicationMonitoringIntegrationTest.MetricsConfiguration.class
})
class EventPublicationMonitoringIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-07-20T10:10:00Z");

  private final EventPublicationBacklogMonitor monitor;
  private final SimpleMeterRegistry registry;
  private final JdbcTemplate jdbc;

  @Autowired
  EventPublicationMonitoringIntegrationTest(
      EventPublicationBacklogMonitor monitor, SimpleMeterRegistry registry, JdbcTemplate jdbc) {
    this.monitor = monitor;
    this.registry = registry;
    this.jdbc = jdbc;
  }

  @BeforeEach
  void deletePublications() {
    jdbc.update("delete from event_publication");
  }

  @Test
  void shouldReflectRegistryBacklogInGauges() {
    insertPublication(Instant.parse("2026-07-20T10:08:00Z"), "PUBLISHED");
    insertPublication(Instant.parse("2026-07-20T10:09:00Z"), "FAILED");

    monitor.refreshBacklogMetrics();

    assertThat(gaugeValue("oven.events.publications.incomplete")).isEqualTo(2);
    assertThat(gaugeValue("oven.events.publications.failed")).isEqualTo(1);
    assertThat(gaugeValue("oven.events.publications.oldest.incomplete.age")).isEqualTo(120);
  }

  private void insertPublication(Instant publicationDate, String status) {
    jdbc.update(
        """
        insert into event_publication (
            id,
            publication_date,
            listener_id,
            serialized_event,
            event_type,
            status
        )
        values (?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        publicationDate.atOffset(ZoneOffset.UTC),
        "test-listener",
        "{}",
        "test.TestEvent",
        status);
  }

  private double gaugeValue(String name) {
    return registry.get(name).gauge().value();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class MetricsConfiguration {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Bean
    SimpleMeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }
}
