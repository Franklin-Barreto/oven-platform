package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventPublisher;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;

  public KafkaOutboxEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public void publish(OutboxEvent event) {
    kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload()).join();
  }
}
