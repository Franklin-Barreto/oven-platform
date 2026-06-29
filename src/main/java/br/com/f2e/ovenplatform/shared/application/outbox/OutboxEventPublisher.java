package br.com.f2e.ovenplatform.shared.application.outbox;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;

public interface OutboxEventPublisher {

  void publish(OutboxEvent event);
}
