package br.com.f2e.ovenplatform.shared.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;

class EventPublicationMaintenanceTest {

  private static final Duration FIXED_DELAY = Duration.ofMinutes(1);
  private static final Duration RETRY_MIN_AGE = Duration.ofSeconds(30);
  private static final Duration COMPLETED_RETENTION = Duration.ofDays(7);

  @Test
  void shouldResubmitEligibleFailuresAndDeleteExpiredCompletions() {
    var failedPublications = mock(FailedEventPublications.class);
    var completedPublications = mock(CompletedEventPublications.class);
    var properties = properties();
    var maintenance =
        new EventPublicationMaintenance(failedPublications, completedPublications, properties);
    var optionsCaptor = ArgumentCaptor.forClass(ResubmissionOptions.class);

    maintenance.maintainPublications();

    verify(failedPublications).resubmit(optionsCaptor.capture());
    verify(completedPublications).deletePublicationsOlderThan(COMPLETED_RETENTION);

    var options = optionsCaptor.getValue();
    assertThat(options.getMinAge()).isEqualTo(RETRY_MIN_AGE);
    assertThat(options.getBatchSize()).isEqualTo(100);
    assertThat(options.getMaxInFlight()).isEqualTo(10);

    var retryable = publicationWithAttempts(4);
    var exhausted = publicationWithAttempts(5);

    assertThat(options.getFilter()).accepts(retryable).rejects(exhausted);
  }

  @ParameterizedTest(name = "rejects invalid maintenance configuration: {0}")
  @MethodSource("invalidProperties")
  void shouldRejectInvalidConfiguration(
      String expectedField,
      Duration fixedDelay,
      Duration retryMinAge,
      int retryBatchSize,
      int retryMaxInFlight,
      int retryMaxAttempts,
      Duration completedRetention) {
    assertThatThrownBy(
            () ->
                new EventPublicationMaintenanceProperties(
                    fixedDelay,
                    retryMinAge,
                    retryBatchSize,
                    retryMaxInFlight,
                    retryMaxAttempts,
                    completedRetention))
        .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class)
        .hasMessageContaining(expectedField);
  }

  private static Stream<Arguments> invalidProperties() {
    return Stream.of(
        Arguments.of("fixedDelay", null, RETRY_MIN_AGE, 100, 10, 5, COMPLETED_RETENTION),
        Arguments.of("retryMinAge", FIXED_DELAY, Duration.ZERO, 100, 10, 5, COMPLETED_RETENTION),
        Arguments.of("retryBatchSize", FIXED_DELAY, RETRY_MIN_AGE, 0, 10, 5, COMPLETED_RETENTION),
        Arguments.of(
            "retryMaxInFlight", FIXED_DELAY, RETRY_MIN_AGE, 100, 0, 5, COMPLETED_RETENTION),
        Arguments.of(
            "retryMaxAttempts", FIXED_DELAY, RETRY_MIN_AGE, 100, 10, 0, COMPLETED_RETENTION),
        Arguments.of(
            "completedRetention", FIXED_DELAY, RETRY_MIN_AGE, 100, 10, 5, Duration.ofDays(-1)));
  }

  private static EventPublicationMaintenanceProperties properties() {
    return new EventPublicationMaintenanceProperties(
        FIXED_DELAY, RETRY_MIN_AGE, 100, 10, 5, COMPLETED_RETENTION);
  }

  private static EventPublication publicationWithAttempts(int attempts) {
    var publication = mock(EventPublication.class);
    when(publication.getCompletionAttempts()).thenReturn(attempts);
    return publication;
  }
}
