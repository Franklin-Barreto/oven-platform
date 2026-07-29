package br.com.f2e.ovenplatform.media.infrastructure.cleanup;

import br.com.f2e.ovenplatform.media.application.StoredImageService;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "oven.media.cleanup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PendingImageCleanup {

  private final StoredImageService service;
  private final PendingImageCleanupProperties properties;
  private final Clock clock;

  PendingImageCleanup(
      StoredImageService service, PendingImageCleanupProperties properties, Clock clock) {
    this.service = service;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(
      initialDelayString = "${oven.media.cleanup.initial-delay}",
      fixedDelayString = "${oven.media.cleanup.fixed-delay}")
  void cleanupPendingImages() {
    var cutoff = clock.instant().minus(properties.pendingRetention());
    service.cleanupPendingImages(cutoff);
  }
}
