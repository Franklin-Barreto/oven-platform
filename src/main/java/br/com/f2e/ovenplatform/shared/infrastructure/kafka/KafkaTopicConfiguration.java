package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "oven.kafka.topics.auto-create", havingValue = "true")
public class KafkaTopicConfiguration {

  private static final int PARTITIONS = 3;
  private static final int REPLICAS = 1;
  private static final String DEAD_LETTER_TOPIC_SUFFIX = "-dlt";

  private final String fulfillmentTopic;
  private final String orderTopic;
  private final String kitchenTopic;

  public KafkaTopicConfiguration(
      @Value("${oven.kafka.topics.kitchen}") String kitchenTopic,
      @Value("${oven.kafka.topics.orders}") String orderTopic,
      @Value("${oven.kafka.topics.fulfillment}") String fulfillmentTopic) {
    this.kitchenTopic = kitchenTopic;
    this.orderTopic = orderTopic;
    this.fulfillmentTopic = fulfillmentTopic;
  }

  @Bean
  NewTopic orderEventsTopic() {
    return topic(orderTopic);
  }

  @Bean
  NewTopic kitchenEventsTopic() {
    return topic(kitchenTopic);
  }

  @Bean
  NewTopic fulfillmentEventsTopic() {
    return topic(fulfillmentTopic);
  }

  @Bean
  NewTopic orderEventsDeadLetterTopic() {
    return deadLetterTopic(orderTopic);
  }

  @Bean
  NewTopic kitchenEventsDeadLetterTopic() {
    return deadLetterTopic(kitchenTopic);
  }

  @Bean
  NewTopic fulfillmentEventsDeadLetterTopic() {
    return deadLetterTopic(fulfillmentTopic);
  }

  private NewTopic deadLetterTopic(String topic) {
    return topic(topic + DEAD_LETTER_TOPIC_SUFFIX);
  }

  private NewTopic topic(String topic) {
    return TopicBuilder.name(topic).partitions(PARTITIONS).replicas(REPLICAS).build();
  }
}
