package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventPublicationBacklogMonitorTest {

  @Mock private JdbcPublicationBacklogReader reader;
  @Mock private EventPublicationMetrics metrics;

  private EventPublicationBacklogMonitor monitor;

  @BeforeEach
  void setUp() {
    monitor = new EventPublicationBacklogMonitor(reader, metrics);
  }

  @Test
  void shouldUpdateMetricsWithCurrentBacklogSnapshot() {
    var snapshot =
        PublicationBacklogSnapshot.withBacklog(2, 1, Instant.parse("2026-07-20T10:00:00Z"));

    when(reader.getSnapshot()).thenReturn(snapshot);

    monitor.refreshBacklogMetrics();

    verify(reader).getSnapshot();
    verify(metrics).update(snapshot);
  }

  @Test
  void shouldNotUpdateMetricsOrPropagateWhenReadingBacklogFails() {
    when(reader.getSnapshot()).thenThrow(new IllegalStateException("Database unavailable"));

    assertThatCode(monitor::refreshBacklogMetrics).doesNotThrowAnyException();

    verify(reader).getSnapshot();
    verifyNoInteractions(metrics);
  }
}
