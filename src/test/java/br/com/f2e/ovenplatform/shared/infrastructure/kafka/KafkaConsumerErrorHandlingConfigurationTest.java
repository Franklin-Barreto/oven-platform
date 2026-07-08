package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

class KafkaConsumerErrorHandlingConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(
              KafkaConsumerErrorHandlingConfiguration.class, TestConfiguration.class)
          .withPropertyValues(
              "oven.kafka.consumer.retry.interval=1s", "oven.kafka.consumer.retry.max-retries=3");

  @Test
  void shouldCreateKafkaConsumerErrorHandlingBeans() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(DefaultErrorHandler.class);
          assertThat(context).hasSingleBean(ConsumerRecordRecoverer.class);
          assertThat(context).hasSingleBean(DeadLetterPublishingRecoverer.class);
        });
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    KafkaTemplate<String, String> kafkaTemplate() {
      return mock();
    }
  }
}
