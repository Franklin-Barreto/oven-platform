package br.com.f2e.ovenplatform.orders.infrastructure.event;

import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FulfillmentOrderReadyEventListenerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Mock private OrderService orderService;

  private FulfillmentOrderReadyEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new FulfillmentOrderReadyEventListener(orderService);
  }

  @Test
  void shouldMapCanonicalFulfillmentEventToOrderReadiness() {
    listener.on(new FulfillmentOrderMarkedAsReadyEvent(TENANT_ID, ORDER_ID, READY_AT));

    verify(orderService).markAsReady(TENANT_ID, ORDER_ID, READY_AT);
  }
}
