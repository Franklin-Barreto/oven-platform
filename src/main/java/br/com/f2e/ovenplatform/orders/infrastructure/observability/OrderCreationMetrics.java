package br.com.f2e.ovenplatform.orders.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class OrderCreationMetrics {

  private final Counter successCounter;
  private final Counter failureCounter;
  private final Timer creationTimer;
  private final MeterRegistry registry;

  public OrderCreationMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.successCounter =
        Counter.builder("oven.orders.creation.successes")
            .description("Number of orders successfully created and committed")
            .register(registry);
    this.failureCounter =
        Counter.builder("oven.orders.creation.failures")
            .description(
                "Number of order creation attempts that failed after reaching the application operation")
            .register(registry);

    this.creationTimer =
        Timer.builder("oven.orders.creation")
            .description(
                "Duration of the transactional order creation operation, including successful and failed attempts")
            .register(registry);
  }

  public Timer.Sample start() {
    return Timer.start(registry);
  }

  public void recordSuccess(Timer.Sample sample) {
    successCounter.increment();
    sample.stop(creationTimer);
  }

  public void recordFailure(Timer.Sample sample) {
    failureCounter.increment();
    sample.stop(creationTimer);
  }
}
