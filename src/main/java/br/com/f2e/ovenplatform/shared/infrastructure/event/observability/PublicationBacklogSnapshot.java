package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.time.Instant;
import java.util.Optional;

record PublicationBacklogSnapshot(
    long incompleteCount, long failedCount, Optional<Instant> oldestPublicationDate) {

  PublicationBacklogSnapshot {
    requireNotNull(oldestPublicationDate, "oldestPublicationDate");

    if (incompleteCount < 0) {
      throw new IllegalArgumentException("incompleteCount must not be negative");
    }

    if (failedCount < 0) {
      throw new IllegalArgumentException("failedCount must not be negative");
    }

    if (failedCount > incompleteCount) {
      throw new IllegalArgumentException("failedCount must not exceed incompleteCount");
    }

    if (incompleteCount == 0 && oldestPublicationDate.isPresent()) {
      throw new IllegalArgumentException(
          "oldestPublicationDate must be empty when incompleteCount is zero");
    }

    if (incompleteCount > 0 && oldestPublicationDate.isEmpty()) {
      throw new IllegalArgumentException(
          "oldestPublicationDate must be present when incompleteCount is positive");
    }
  }

  static PublicationBacklogSnapshot empty() {
    return new PublicationBacklogSnapshot(0, 0, Optional.empty());
  }

  static PublicationBacklogSnapshot withBacklog(long count, long failedCount, Instant oldest) {
    return new PublicationBacklogSnapshot(count, failedCount, Optional.of(oldest));
  }
}
