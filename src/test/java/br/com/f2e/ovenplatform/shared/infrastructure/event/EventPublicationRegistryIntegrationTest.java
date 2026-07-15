package br.com.f2e.ovenplatform.shared.infrastructure.event;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    classes = EventPublicationRegistryIntegrationTest.TestApplication.class,
    properties = "oven.events.publication.maintenance.enabled=false")
@Import(PostgresTestContainerConfiguration.class)
class EventPublicationRegistryIntegrationTest {

  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);

  private final ApplicationEventPublisher eventPublisher;
  private final EventPublicationRegistry registry;
  private final FailedEventPublications failedPublications;
  private final TransactionTemplate transactions;
  private final JdbcTemplate jdbc;
  private final EventProbe probe;

  @Autowired
  EventPublicationRegistryIntegrationTest(
      ApplicationEventPublisher eventPublisher,
      EventPublicationRegistry registry,
      FailedEventPublications failedPublications,
      PlatformTransactionManager transactionManager,
      JdbcTemplate jdbc,
      EventProbe probe) {
    this.eventPublisher = eventPublisher;
    this.registry = registry;
    this.failedPublications = failedPublications;
    this.transactions = new TransactionTemplate(transactionManager);
    this.jdbc = jdbc;
    this.probe = probe;
  }

  @BeforeEach
  void resetPublications() {
    jdbc.update("delete from event_publication");
    probe.reset();
  }

  @Test
  void shouldActivateJpaRegistryWithLiquibaseOwnedPostgresSchema() {
    assertThat(registry).isNotNull();

    var timestampColumns =
        jdbc.query(
            """
            select column_name, data_type
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'event_publication'
              and column_name in (
                'publication_date', 'completion_date', 'last_resubmission_date'
              )
            """,
            resultSet -> {
              var result = new java.util.HashMap<String, String>();
              while (resultSet.next()) {
                result.put(resultSet.getString("column_name"), resultSet.getString("data_type"));
              }
              return result;
            });

    assertThat(timestampColumns)
        .containsOnly(
            Map.entry("publication_date", "timestamp with time zone"),
            Map.entry("completion_date", "timestamp with time zone"),
            Map.entry("last_resubmission_date", "timestamp with time zone"));

    assertThat(indexNames())
        .contains(
            "event_publication_by_completion_date_idx",
            "event_publication_serialized_event_hash_idx");
  }

  @Test
  void shouldRemoveLegacyOutboxTable() {
    var legacyTableCount =
        requireNonNull(
            jdbc.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name = 'outbox_events'
                """,
                Integer.class),
            "Legacy outbox table count not returned");

    assertThat(legacyTableCount).isZero();
  }

  @Test
  void shouldPersistAndCompleteTransactionalApplicationModuleEvent() throws Exception {
    var event = probe.prepare(0);

    publishInTransaction(event);

    assertThat(probe.awaitSuccess(event.id(), ASYNC_TIMEOUT)).isTrue();
    awaitStatus(event.id(), EventPublication.Status.COMPLETED);
    assertThat(completionAttempts(event.id())).isOne();
  }

  @Test
  void shouldRecoverFailedPublicationByResubmittingIt() throws Exception {
    var event = probe.prepare(1);

    publishInTransaction(event);
    awaitStatus(event.id(), EventPublication.Status.FAILED);

    failedPublications.resubmit(ResubmissionOptions.defaults().withMinAge(Duration.ZERO));

    assertThat(probe.awaitSuccess(event.id(), ASYNC_TIMEOUT)).isTrue();
    awaitStatus(event.id(), EventPublication.Status.COMPLETED);
    assertThat(completionAttempts(event.id())).isEqualTo(2);
  }

  @Test
  void shouldRollBackPublicationWithOriginatingTransaction() throws Exception {
    var event = probe.prepare(0);

    transactions.executeWithoutResult(
        status -> {
          eventPublisher.publishEvent(event);
          status.setRollbackOnly();
        });

    assertThat(probe.awaitSuccess(event.id(), Duration.ofMillis(300))).isFalse();
    assertThat(publicationCount(event.id())).isZero();
    assertThat(probe.attempts(event.id())).isZero();
  }

  private void publishInTransaction(TestModuleEvent event) {
    transactions.executeWithoutResult(_ -> eventPublisher.publishEvent(event));
  }

  private void awaitStatus(UUID eventId, EventPublication.Status expected) {
    await()
        .atMost(ASYNC_TIMEOUT)
        .pollInterval(Duration.ofMillis(25))
        .untilAsserted(() -> assertThat(publicationStatuses(eventId)).contains(expected.name()));
  }

  private List<String> publicationStatuses(UUID eventId) {
    return jdbc.queryForList(
        "select status from event_publication where serialized_event like ?",
        String.class,
        serializedEventPattern(eventId));
  }

  private int completionAttempts(UUID eventId) {
    return requireNonNull(
        jdbc.queryForObject(
            """
                    select completion_attempts
                    from event_publication
                    where serialized_event like ?
                    """,
            Integer.class,
            serializedEventPattern(eventId)),
        "Completion attempts not found for event " + eventId);
  }

  private int publicationCount(UUID eventId) {
    return requireNonNull(
        jdbc.queryForObject(
            "select count(*) from event_publication where serialized_event like ?",
            Integer.class,
            serializedEventPattern(eventId)),
        "Publication count not returned for event " + eventId);
  }

  private java.util.List<String> indexNames() {
    return jdbc.queryForList(
        "select indexname from pg_indexes where schemaname = current_schema() and tablename = 'event_publication'",
        String.class);
  }

  private String serializedEventPattern(UUID eventId) {
    return "%" + eventId + "%";
  }

  public record TestModuleEvent(UUID id) {}

  static class DurableTestEventListener {

    private final EventProbe probe;

    DurableTestEventListener(EventProbe probe) {
      this.probe = probe;
    }

    @ApplicationModuleListener(id = "issue-161-test-listener")
    void on(TestModuleEvent event) {
      probe.handle(event);
    }
  }

  static class EventProbe {

    private final Map<UUID, ProbeState> states = new ConcurrentHashMap<>();

    TestModuleEvent prepare(int failuresBeforeSuccess) {
      var event = new TestModuleEvent(UUID.randomUUID());
      states.put(event.id(), new ProbeState(failuresBeforeSuccess));
      return event;
    }

    void handle(TestModuleEvent event) {
      var state = states.get(event.id());
      if (state == null) {
        throw new IllegalStateException("No probe registered for event " + event.id());
      }

      state.attempts.incrementAndGet();
      if (state.failuresRemaining.getAndUpdate(current -> Math.max(0, current - 1)) > 0) {
        throw new IllegalStateException("Simulated listener failure");
      }

      state.success.countDown();
    }

    boolean awaitSuccess(UUID eventId, Duration timeout) throws InterruptedException {
      return states.get(eventId).success.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    int attempts(UUID eventId) {
      return states.get(eventId).attempts.get();
    }

    void reset() {
      states.clear();
    }

    private static final class ProbeState {
      private final AtomicInteger failuresRemaining;
      private final AtomicInteger attempts = new AtomicInteger();
      private final CountDownLatch success = new CountDownLatch(1);

      private ProbeState(int failuresBeforeSuccess) {
        this.failuresRemaining = new AtomicInteger(failuresBeforeSuccess);
      }
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestEventConfiguration {

    @Bean
    EventProbe eventProbe() {
      return new EventProbe();
    }

    @Bean
    DurableTestEventListener durableTestEventListener(EventProbe probe) {
      return new DurableTestEventListener(probe);
    }
  }

  @SpringBootApplication
  @Import(TestEventConfiguration.class)
  static class TestApplication {}
}
