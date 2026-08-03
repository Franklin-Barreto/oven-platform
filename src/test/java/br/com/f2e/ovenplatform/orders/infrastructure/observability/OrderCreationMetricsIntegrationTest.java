package br.com.f2e.ovenplatform.orders.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.CreateOrderItemCommand;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfoProvider;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.OrderableProduct;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.application.OrderableProductSelection;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.application.ProductNotAvailableForOrderingException;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentProcessingMode;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ImportAutoConfiguration({AopAutoConfiguration.class, JacksonAutoConfiguration.class})
@Import({
  OrderService.class,
  JpaOrderRepositoryAdapter.class,
  OrderCreationMetrics.class,
  OrderCreationMetricsAspect.class,
  OrderCreationMetricsIntegrationTest.MetricsTestConfiguration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderCreationMetricsIntegrationTest extends DataJpaIntegrationTest {

  private static final String CREATED_METRIC = "oven.orders.creation.successes";
  private static final String CREATION_FAILURES_METRIC = "oven.orders.creation.failures";
  private static final String CREATION_TIMER = "oven.orders.creation";

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aedc");

  private static final UUID PRODUCT_ID = UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final Instant ORDER_CREATION_INSTANT = Instant.parse("2026-08-03T12:00:00Z");

  @Autowired private OrderService orderService;
  @Autowired private SimpleMeterRegistry meterRegistry;

  @MockitoBean private OrderableProductProvider orderableProductProvider;
  @MockitoBean private CustomerDeliveryInfoProvider customerDeliveryInfoProvider;
  @MockitoBean private Clock clock;

  @BeforeEach
  void configureClock() {
    when(clock.instant()).thenReturn(ORDER_CREATION_INSTANT);
  }

  @Test
  void shouldRecordSuccessfulOrderCreationThroughSpringProxy() {
    var successCountBefore = counterValue(CREATED_METRIC);
    var failureCountBefore = counterValue(CREATION_FAILURES_METRIC);
    var timerCountBefore = timerCount();
    var command = createOrderCommand();

    when(orderableProductProvider.findOrderableProducts(
            TENANT_ID, List.of(new OrderableProductSelection(PRODUCT_ID, null))))
        .thenReturn(
            List.of(new OrderableProduct(PRODUCT_ID, "Pizza Portuguesa", new BigDecimal("35.40"))));

    orderService.createOrder(TENANT_ID, command);

    assertThat(counterValue(CREATED_METRIC)).isEqualTo(successCountBefore + 1);
    assertThat(counterValue(CREATION_FAILURES_METRIC)).isEqualTo(failureCountBefore);
    assertThat(timerCount()).isEqualTo(timerCountBefore + 1);
  }

  @Test
  void shouldRecordFailedOrderCreationThroughSpringProxy() {
    var successCountBefore = counterValue(CREATED_METRIC);
    var failureCountBefore = counterValue(CREATION_FAILURES_METRIC);
    var timerCountBefore = timerCount();
    var command = createOrderCommand();

    when(orderableProductProvider.findOrderableProducts(
            TENANT_ID, List.of(new OrderableProductSelection(PRODUCT_ID, null))))
        .thenReturn(List.of());

    assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, command))
        .isInstanceOf(ProductNotAvailableForOrderingException.class);

    assertThat(counterValue(CREATED_METRIC)).isEqualTo(successCountBefore);
    assertThat(counterValue(CREATION_FAILURES_METRIC)).isEqualTo(failureCountBefore + 1);
    assertThat(timerCount()).isEqualTo(timerCountBefore + 1);
  }

  private CreateOrderCommand createOrderCommand() {
    return new CreateOrderCommand(
        List.of(new CreateOrderItemCommand(PRODUCT_ID, 1)),
        new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID, PaymentProcessingMode.MANUAL),
        OrderServiceType.COUNTER);
  }

  private double counterValue(String metricName) {
    return meterRegistry.get(metricName).counter().count();
  }

  private long timerCount() {
    return meterRegistry.get(CREATION_TIMER).timer().count();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class MetricsTestConfiguration {

    @Bean
    SimpleMeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }
}
