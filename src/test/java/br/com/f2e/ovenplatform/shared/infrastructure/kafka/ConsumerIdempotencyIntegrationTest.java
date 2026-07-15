package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfoProvider;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.kafka.FulfillmentOrderReadyConsumer;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({FulfillmentOrderReadyConsumer.class, JpaOrderRepositoryAdapter.class, OrderService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConsumerIdempotencyIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Autowired private FulfillmentOrderReadyConsumer orderConsumer;
  @Autowired private OrderService orderService;

  @MockitoBean private Clock clock;
  @MockitoBean private OrderableProductProvider orderableProductProvider;
  @MockitoBean private CustomerDeliveryInfoProvider customerDeliveryInfoProvider;

  @Test
  void shouldPreserveOriginalReadyAtWhenFulfillmentOrderReadyDeliveryIsRepeated() {
    var order = orderService.save(new Order(TENANT_ID, OrderServiceType.COUNTER));
    var repeatedReadyAt = Instant.parse("2026-05-12T20:45:00Z");

    orderConsumer.on(
        JsonUtils.toJson(new FulfillmentOrderReadyPayload(TENANT_ID, order.getId(), READY_AT)));
    orderConsumer.on(
        JsonUtils.toJson(
            new FulfillmentOrderReadyPayload(TENANT_ID, order.getId(), repeatedReadyAt)));

    var persistedOrder = orderService.findOrder(TENANT_ID, order.getId()).orElseThrow();

    assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.READY);
    assertThat(persistedOrder.getReadyAt()).isEqualTo(READY_AT);
  }
}
