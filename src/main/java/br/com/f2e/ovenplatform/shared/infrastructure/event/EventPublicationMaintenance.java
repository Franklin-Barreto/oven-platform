package br.com.f2e.ovenplatform.shared.infrastructure.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(EventPublicationMaintenanceProperties.class)
@ConditionalOnProperty(
    prefix = "oven.events.publication.maintenance",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class EventPublicationMaintenance {

  private final FailedEventPublications failedPublications;
  private final CompletedEventPublications completedPublications;
  private final EventPublicationMaintenanceProperties properties;
  private final EventPublicationMaintenanceMetrics metrics;

  EventPublicationMaintenance(
      FailedEventPublications failedPublications,
      CompletedEventPublications completedPublications,
      EventPublicationMaintenanceProperties properties,
      EventPublicationMaintenanceMetrics metrics) {
    this.failedPublications = failedPublications;
    this.completedPublications = completedPublications;
    this.properties = properties;
    this.metrics = metrics;
  }

  @Scheduled(fixedDelayString = "${oven.events.publication.maintenance.fixed-delay}")
  void maintainPublications() {
    resubmitFailedPublications();
    deleteCompletedPublications();
  }

  private void resubmitFailedPublications() {
    try {
      failedPublications.resubmit(
          ResubmissionOptions.defaults()
              .withMinAge(properties.retryMinAge())
              .withBatchSize(properties.retryBatchSize())
              .withMaxInFlight(properties.retryMaxInFlight())
              .withFilter(
                  publication ->
                      publication.getCompletionAttempts() < properties.retryMaxAttempts()));

      metrics.recordResubmissionSuccess();
    } catch (RuntimeException exception) {
      metrics.recordResubmissionFailure();
      throw exception;
    }
  }

  private void deleteCompletedPublications() {
    try {
      completedPublications.deletePublicationsOlderThan(properties.completedRetention());
      metrics.recordCleanupSuccess();
    } catch (RuntimeException exception) {
      metrics.recordCleanupFailure();
      throw exception;
    }
  }
}
