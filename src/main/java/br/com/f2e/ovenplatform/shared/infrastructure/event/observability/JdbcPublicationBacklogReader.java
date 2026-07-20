package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static java.util.Objects.requireNonNull;

import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPublicationBacklogReader {

  private static final String BACKLOG_SNAPSHOT_QUERY =
      """
          select count(*) as incomplete_count,
                 count(*) filter (where status = 'FAILED') as failed_count,
                 min(publication_date) as oldest_publication_date
          from event_publication
          where completion_date is null
          """;

  private final JdbcTemplate jdbcTemplate;

  JdbcPublicationBacklogReader(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
  }

  PublicationBacklogSnapshot getSnapshot() {
    var snapshot =
        jdbcTemplate.queryForObject(
            BACKLOG_SNAPSHOT_QUERY,
            (resultSet, _) -> {
              var count = resultSet.getLong("incomplete_count");
              var failedCount = resultSet.getLong("failed_count");
              var oldest = resultSet.getObject("oldest_publication_date", OffsetDateTime.class);

              if (count == 0) {
                return PublicationBacklogSnapshot.empty();
              }

              requireNonNull(
                  oldest, "oldestPublicationDate must be present when incompleteCount is positive");

              return PublicationBacklogSnapshot.withBacklog(count, failedCount, oldest.toInstant());
            });

    return requireNonNull(snapshot, "Backlog snapshot query must return an aggregate result");
  }
}
