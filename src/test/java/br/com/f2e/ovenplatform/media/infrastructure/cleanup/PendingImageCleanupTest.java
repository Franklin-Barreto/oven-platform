package br.com.f2e.ovenplatform.media.infrastructure.cleanup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.media.application.StoredImageService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingImageCleanupTest {

  private static final Instant NOW = Instant.parse("2026-07-29T12:00:00Z");
  private static final Duration PENDING_RETENTION = Duration.ofHours(24);

  @Test
  void shouldCleanImagesOlderThanConfiguredRetention() {
    var service = mock(StoredImageService.class);
    var clock = Clock.fixed(NOW, ZoneOffset.UTC);
    var properties =
        new PendingImageCleanupProperties(
            Duration.ofMinutes(1), Duration.ofHours(1), PENDING_RETENTION);

    var cleanup = new PendingImageCleanup(service, properties, clock);
    cleanup.cleanupPendingImages();

    verify(service).cleanupPendingImages(NOW.minus(PENDING_RETENTION));
  }

  @Test
  void shouldAllowImmediateInitialExecution() {
    assertThatCode(
            () ->
                new PendingImageCleanupProperties(
                    Duration.ZERO, Duration.ofHours(1), Duration.ofHours(24)))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest(name = "rejects invalid cleanup configuration: {0}")
  @MethodSource("invalidProperties")
  void shouldRejectInvalidConfiguration(
      String expectedField, Duration initialDelay, Duration fixedDelay, Duration pendingRetention) {

    assertThatThrownBy(
            () -> new PendingImageCleanupProperties(initialDelay, fixedDelay, pendingRetention))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedField);
  }

  private static Stream<Arguments> invalidProperties() {
    var positive = Duration.ofMinutes(1);

    return Stream.of(
        Arguments.of("initialDelay", null, positive, positive),
        Arguments.of("initialDelay", Duration.ofSeconds(-1), positive, positive),
        Arguments.of("fixedDelay", Duration.ZERO, Duration.ZERO, positive),
        Arguments.of("fixedDelay", Duration.ZERO, Duration.ofSeconds(-1), positive),
        Arguments.of("pendingRetention", Duration.ZERO, positive, Duration.ZERO),
        Arguments.of("pendingRetention", Duration.ZERO, positive, Duration.ofSeconds(-1)));
  }
}
