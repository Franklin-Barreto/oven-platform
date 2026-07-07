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
    return TopicBuilder.name(orderTopic).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic kitchenEventsTopic() {
    return TopicBuilder.name(kitchenTopic).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic fulfillmentEventsTopic() {
    return TopicBuilder.name(fulfillmentTopic).partitions(3).replicas(1).build();
  }
}
