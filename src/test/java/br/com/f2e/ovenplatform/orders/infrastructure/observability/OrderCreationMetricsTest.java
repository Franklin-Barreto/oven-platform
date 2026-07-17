package br.com.f2e.ovenplatform.orders.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderCreationMetricsTest {

  private static final String OVEN_ORDERS_CREATED = "oven.orders.creation.successes";
  private static final String OVEN_ORDERS_CREATION_FAILURES = "oven.orders.creation.failures";
  private static final String OVEN_ORDERS_CREATION = "oven.orders.creation";
  private SimpleMeterRegistry simpleMeterRegistry;
  private OrderCreationMetrics orderCreationMetrics;

  @BeforeEach
  void setUp() {
    simpleMeterRegistry = new SimpleMeterRegistry();
    orderCreationMetrics = new OrderCreationMetrics(simpleMeterRegistry);
  }

  @Test
  void shouldIncrementSuccessCounterAndRecordCreationTime() {
    var sample = orderCreationMetrics.start();
    orderCreationMetrics.recordSuccess(sample);

    var successCount = simpleMeterRegistry.get(OVEN_ORDERS_CREATED).counter().count();
    var failureCount = simpleMeterRegistry.get(OVEN_ORDERS_CREATION_FAILURES).counter().count();
    var timerCount = simpleMeterRegistry.get(OVEN_ORDERS_CREATION).timer().count();

    assertThat(successCount).isEqualTo(1);
    assertThat(failureCount).isZero();
    assertThat(timerCount).isEqualTo(1);
  }

  @Test
  void shouldIncrementFailureCounterAndRecordCreationTime() {
    var sample = orderCreationMetrics.start();
    orderCreationMetrics.recordFailure(sample);

    var successCount = simpleMeterRegistry.get(OVEN_ORDERS_CREATED).counter().count();
    var failureCount = simpleMeterRegistry.get(OVEN_ORDERS_CREATION_FAILURES).counter().count();
    var timerCount = simpleMeterRegistry.get(OVEN_ORDERS_CREATION).timer().count();

    assertThat(failureCount).isEqualTo(1);
    assertThat(successCount).isZero();
    assertThat(timerCount).isEqualTo(1);
  }
}
