package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestContainerConfiguration.class, JdbcPublicationBacklogReader.class})
class JdbcPublicationBacklogReaderTest {

  private final JdbcPublicationBacklogReader reader;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  JdbcPublicationBacklogReaderTest(JdbcPublicationBacklogReader reader, JdbcTemplate jdbcTemplate) {
    this.reader = reader;
    this.jdbcTemplate = jdbcTemplate;
  }

  @BeforeEach
  void deletePublications() {
    jdbcTemplate.update("delete from event_publication");
  }

  @Test
  void shouldReturnEmptySnapshotWhenThereAreNoIncompletePublications() {
    var snapshot = reader.getSnapshot();

    assertThat(snapshot.incompleteCount()).isZero();
    assertThat(snapshot.failedCount()).isZero();
    assertThat(snapshot.oldestPublicationDate()).isEmpty();
  }

  @Test
  void shouldCountIncompletePublicationsAndReturnTheOldestPublicationDate() {
    var oldest = Instant.parse("2026-07-20T10:00:00Z");
    var newest = Instant.parse("2026-07-20T10:05:00Z");

    insertPublication(oldest, null, "PUBLISHED");
    insertPublication(newest, null, "FAILED");

    var snapshot = reader.getSnapshot();

    assertThat(snapshot.incompleteCount()).isEqualTo(2);
    assertThat(snapshot.failedCount()).isOne();
    assertThat(snapshot.oldestPublicationDate()).contains(oldest);
  }

  @Test
  void shouldIgnoreCompletedPublications() {
    var completedPublicationDate = Instant.parse("2026-07-20T09:00:00Z");
    var completionDate = Instant.parse("2026-07-20T09:01:00Z");
    var incompletePublicationDate = Instant.parse("2026-07-20T10:00:00Z");

    insertPublication(completedPublicationDate, completionDate, "COMPLETED");
    insertPublication(incompletePublicationDate, null, "PUBLISHED");

    var snapshot = reader.getSnapshot();

    assertThat(snapshot.incompleteCount()).isOne();
    assertThat(snapshot.failedCount()).isZero();
    assertThat(snapshot.oldestPublicationDate()).contains(incompletePublicationDate);
  }

  private void insertPublication(Instant publicationDate, Instant completionDate, String status) {

    jdbcTemplate.update(
        """
            insert into event_publication (
                id,
                publication_date,
                listener_id,
                serialized_event,
                event_type,
                completion_date,
                status
            )
            values (?, ?, ?, ?, ?, ?, ?)
            """,
        UUID.randomUUID(),
        toOffsetDateTime(publicationDate),
        "test-listener",
        "{}",
        "test.TestEvent",
        completionDate == null ? null : toOffsetDateTime(completionDate),
        status);
  }

  private OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
