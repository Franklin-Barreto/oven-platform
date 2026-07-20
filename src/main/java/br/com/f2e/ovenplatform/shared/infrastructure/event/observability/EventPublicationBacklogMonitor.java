package br.com.f2e.ovenplatform.shared.infrastructure.event.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class EventPublicationBacklogMonitor {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(EventPublicationBacklogMonitor.class);

  private final JdbcPublicationBacklogReader reader;
  private final EventPublicationMetrics metrics;

  EventPublicationBacklogMonitor(
      JdbcPublicationBacklogReader reader, EventPublicationMetrics metrics) {
    this.reader = reader;
    this.metrics = metrics;
  }

  @Scheduled(fixedDelayString = "${oven.events.publication.monitoring.fixed-delay:1m}")
  void refreshBacklogMetrics() {
    try {
      var snapshot = reader.getSnapshot();
      metrics.update(snapshot);
    } catch (RuntimeException exception) {
      LOGGER.error("Failed to refresh event publication backlog metrics", exception);
    }
  }
}
