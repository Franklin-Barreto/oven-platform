package br.com.f2e.ovenplatform.shared.infrastructure.outbox;

import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublishingSchedulerTest {

  @Mock private OutboxPublisher publisher;

  @Test
  void shouldDelegatePendingEventPublishingToOutboxPublisher() {
    var scheduler = new OutboxEventPublishingScheduler(publisher);

    scheduler.publishPendingEvents();

    verify(publisher).publishPendingEvents();
  }
}
