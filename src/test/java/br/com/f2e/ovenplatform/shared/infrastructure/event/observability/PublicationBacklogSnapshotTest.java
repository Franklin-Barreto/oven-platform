package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PublicationBacklogSnapshotTest {

  private static final Instant OLDEST = Instant.parse("2026-07-20T10:00:00Z");

  @Test
  void shouldCreateEmptySnapshot() {
    var snapshot = PublicationBacklogSnapshot.empty();

    assertThat(snapshot.incompleteCount()).isZero();
    assertThat(snapshot.failedCount()).isZero();
    assertThat(snapshot.oldestPublicationDate()).isEmpty();
  }

  @Test
  void shouldCreateSnapshotWithBacklog() {
    var snapshot = PublicationBacklogSnapshot.withBacklog(3, 1, OLDEST);

    assertThat(snapshot.incompleteCount()).isEqualTo(3);
    assertThat(snapshot.failedCount()).isOne();
    assertThat(snapshot.oldestPublicationDate()).contains(OLDEST);
  }

  @Test
  void shouldRejectNegativeCounts() {
    assertThatThrownBy(() -> new PublicationBacklogSnapshot(-1, 0, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("incompleteCount");

    assertThatThrownBy(() -> new PublicationBacklogSnapshot(1, -1, Optional.of(OLDEST)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("failedCount");
  }

  @Test
  void shouldRejectFailedCountGreaterThanIncompleteCount() {
    assertThatThrownBy(() -> new PublicationBacklogSnapshot(1, 2, Optional.of(OLDEST)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("failedCount");
  }

  @Test
  void shouldRejectDateWhenBacklogIsEmpty() {
    assertThatThrownBy(() -> new PublicationBacklogSnapshot(0, 0, Optional.of(OLDEST)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be empty");
  }

  @Test
  void shouldRequireDateWhenBacklogExists() {
    assertThatThrownBy(() -> new PublicationBacklogSnapshot(1, 0, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be present");
  }
}
