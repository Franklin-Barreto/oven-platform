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

  public EventPublicationMaintenance(
      FailedEventPublications failedPublications,
      CompletedEventPublications completedPublications,
      EventPublicationMaintenanceProperties properties) {
    this.failedPublications = failedPublications;
    this.completedPublications = completedPublications;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${oven.events.publication.maintenance.fixed-delay}")
  void maintainPublications() {
    failedPublications.resubmit(
        ResubmissionOptions.defaults()
            .withMinAge(properties.retryMinAge())
            .withBatchSize(properties.retryBatchSize())
            .withMaxInFlight(properties.retryMaxInFlight())
            .withFilter(
                publication ->
                    publication.getCompletionAttempts() < properties.retryMaxAttempts()));

    completedPublications.deletePublicationsOlderThan(properties.completedRetention());
  }
}
