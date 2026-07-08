package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableConfigurationProperties(KafkaConsumerRetryProperties.class)
public class KafkaConsumerErrorHandlingConfiguration {

  private final KafkaConsumerRetryProperties retryProperties;

  public KafkaConsumerErrorHandlingConfiguration(KafkaConsumerRetryProperties retryProperties) {
    this.retryProperties = retryProperties;
  }

  @Bean
  public DefaultErrorHandler kafkaConsumerErrorHandler(ConsumerRecordRecoverer recoverer) {
    var handler =
        new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(retryProperties.interval().toMillis(), retryProperties.maxRetries()));
    handler.addNotRetryableExceptions(IllegalArgumentException.class);
    return handler;
  }

  @Bean
  public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
      KafkaTemplate<String, String> kafkaTemplate) {
    return new DeadLetterPublishingRecoverer(kafkaTemplate);
  }
}
