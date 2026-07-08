package br.com.f2e.ovenplatform.shared.infrastructure.outbox;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "oven.outbox.publishing",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxEventPublishingScheduler {

  private final OutboxPublisher publisher;

  public OutboxEventPublishingScheduler(OutboxPublisher publisher) {
    this.publisher = publisher;
  }

  @Scheduled(fixedDelayString = "${oven.outbox.publishing.fixed-delay}")
  void publishPendingEvents() {
    publisher.publishPendingEvents();
  }
}
