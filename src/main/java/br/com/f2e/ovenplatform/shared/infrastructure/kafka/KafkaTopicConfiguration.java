package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.TOPIC;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "oven.kafka.topics.auto-create", havingValue = "true")
public class KafkaTopicConfiguration {

  @Bean
  NewTopic orderEventsTopic() {
    return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
  }
}
