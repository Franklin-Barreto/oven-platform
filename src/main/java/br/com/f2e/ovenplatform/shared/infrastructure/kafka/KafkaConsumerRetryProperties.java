package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oven.kafka.consumer.retry")
public record KafkaConsumerRetryProperties(Duration interval, long maxRetries) {}
