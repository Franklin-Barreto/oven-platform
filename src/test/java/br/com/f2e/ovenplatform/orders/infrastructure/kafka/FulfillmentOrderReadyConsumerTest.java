package br.com.f2e.ovenplatform.orders.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FulfillmentOrderReadyConsumerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Mock private OrderService orderService;

  private FulfillmentOrderReadyConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new FulfillmentOrderReadyConsumer(orderService, JsonUtils.getObjectMapper());
  }

  @Test
  void shouldMarkOrderAsReadyWhenFulfillmentOrderReadyEventIsConsumed() {
    var payload = new FulfillmentOrderReadyPayload(TENANT_ID, ORDER_ID, READY_AT);
    var json = JsonUtils.toJson(payload);

    consumer.on(json);
    verify(orderService).markAsReady(TENANT_ID, ORDER_ID, READY_AT);
  }

  @Test
  void shouldFailWhenFulfillmentOrderReadyPayloadDoesNotContainReadyAt() {
    var json =
        """
            {
              "tenantId": "a6210129-f1d5-4942-8d0a-b144e518aecc",
              "orderId": "bb210129-f1d5-4942-8d0a-b144e518aecd"
            }
            """;

    assertThatThrownBy(() -> consumer.on(json)).hasMessageContaining("readyAt must not be null");

    verifyNoInteractions(orderService);
  }
}
