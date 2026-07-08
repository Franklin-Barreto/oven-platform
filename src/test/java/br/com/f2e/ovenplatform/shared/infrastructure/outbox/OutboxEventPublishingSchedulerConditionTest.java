package br.com.f2e.ovenplatform.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.f2e.ovenplatform.shared.application.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class OutboxEventPublishingSchedulerConditionTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(OutboxEventPublishingScheduler.class, TestConfiguration.class)
          .withPropertyValues("oven.outbox.publishing.fixed-delay=5s");

  @Test
  void shouldCreateSchedulerWhenPublishingIsEnabled() {
    contextRunner
        .withPropertyValues("oven.outbox.publishing.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(OutboxEventPublishingScheduler.class));
  }

  @Test
  void shouldNotCreateSchedulerWhenPublishingIsDisabled() {
    contextRunner
        .withPropertyValues("oven.outbox.publishing.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(OutboxEventPublishingScheduler.class));
  }

  @Test
  void shouldCreateSchedulerWhenPublishingEnabledPropertyIsMissing() {
    contextRunner.run(
        context -> assertThat(context).hasSingleBean(OutboxEventPublishingScheduler.class));
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    OutboxPublisher outboxPublisher() {
      return mock(OutboxPublisher.class);
    }
  }
}
