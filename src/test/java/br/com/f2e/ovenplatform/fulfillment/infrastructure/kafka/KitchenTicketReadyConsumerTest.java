package br.com.f2e.ovenplatform.fulfillment.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.fulfillment.application.FulfillmentService;
import br.com.f2e.ovenplatform.fulfillment.application.PreparationReadyCommand;
import br.com.f2e.ovenplatform.shared.application.event.payload.KitchenTicketReadyPayload;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KitchenTicketReadyConsumerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID TICKET_ID = UUID.fromString("b7210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Mock private FulfillmentService fulfillmentService;

  private KitchenTicketReadyConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new KitchenTicketReadyConsumer(fulfillmentService, JsonUtils.getObjectMapper());
  }

  @Test
  void shouldHandleKitchenTicketReadyPayloadAsPreparationReadyCommand() {
    var payload = new KitchenTicketReadyPayload(TENANT_ID, TICKET_ID, ORDER_ID, READY_AT);
    var json = JsonUtils.toJson(payload);

    consumer.on(json);

    var commandCaptor = ArgumentCaptor.forClass(PreparationReadyCommand.class);
    verify(fulfillmentService).handlePreparationReady(commandCaptor.capture());

    var command = commandCaptor.getValue();

    assertThat(command.tenantId()).isEqualTo(TENANT_ID);
    assertThat(command.orderId()).isEqualTo(ORDER_ID);
    assertThat(command.readyAt()).isEqualTo(READY_AT);
  }
}
