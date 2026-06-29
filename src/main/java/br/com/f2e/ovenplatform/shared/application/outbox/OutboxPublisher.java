package br.com.f2e.ovenplatform.shared.application.outbox;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisher {

  private static final int DEFAULT_BATCH_SIZE = 100;

  private final OutboxEventRepository repository;
  private final OutboxEventPublisher publisher;
  private final Clock clock;

  public OutboxPublisher(
      OutboxEventRepository repository, OutboxEventPublisher publisher, Clock clock) {
    this.repository = repository;
    this.publisher = publisher;
    this.clock = clock;
  }

  @Transactional
  public void publishPendingEvents() {
    var events = repository.findPendingEvents(DEFAULT_BATCH_SIZE);

    if (events.isEmpty()) {
      return;
    }

    events.forEach(
        event -> {
          try {
            publisher.publish(event);
            event.markAsPublished(clock.instant());
          } catch (RuntimeException exception) {
            event.markAsFailed(
                exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage());
          }
        });
    repository.saveAll(events);
  }
}
