package br.com.f2e.ovenplatform.kitchen.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.infrastructure.kafka.payload.OrderCreatedItemPayload;
import br.com.f2e.ovenplatform.kitchen.infrastructure.kafka.payload.OrderCreatedPayload;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreatedKitchenTicketConsumerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final UUID SECOND_PRODUCT_ID =
      UUID.fromString("c6c7d4e3-4f70-46d6-9b5c-9e7e9b0d5678");

  private static final String PRODUCT_NAME = "Pizza Portuguesa";
  private static final String SECOND_PRODUCT_NAME = "Pizza Calabresa";

  @Mock private KitchenService kitchenService;

  private OrderCreatedKitchenTicketConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new OrderCreatedKitchenTicketConsumer(kitchenService, JsonUtils.getObjectMapper());
  }

  @Test
  void shouldCreateKitchenTicketFromOrderCreatedPayload() {
    var payload =
        new OrderCreatedPayload(
            TENANT_ID,
            ORDER_ID,
            List.of(
                new OrderCreatedItemPayload(PRODUCT_ID, PRODUCT_NAME, 2),
                new OrderCreatedItemPayload(SECOND_PRODUCT_ID, SECOND_PRODUCT_NAME, 1)));

    var json = JsonUtils.toJson(payload);

    consumer.on(json);

    var commandCaptor = ArgumentCaptor.forClass(CreateTicketCommand.class);

    verify(kitchenService).createTicketFromOrder(commandCaptor.capture());

    var command = commandCaptor.getValue();

    assertThat(command.tenantId()).isEqualTo(TENANT_ID);
    assertThat(command.orderId()).isEqualTo(ORDER_ID);

    assertThat(command.items())
        .hasSize(2)
        .satisfiesExactly(
            firstItem -> {
              assertThat(firstItem.productId()).isEqualTo(PRODUCT_ID);
              assertThat(firstItem.productName()).isEqualTo(PRODUCT_NAME);
              assertThat(firstItem.quantity()).isEqualTo(2);
            },
            secondItem -> {
              assertThat(secondItem.productId()).isEqualTo(SECOND_PRODUCT_ID);
              assertThat(secondItem.productName()).isEqualTo(SECOND_PRODUCT_NAME);
              assertThat(secondItem.quantity()).isEqualTo(1);
            });
  }

  @Test
  void shouldDeserializeEnrichedOrderCreatedPayloadAndCreateKitchenTicket() {

    var json =
        """
      {
        "tenantId": "a6210129-f1d5-4942-8d0a-b144e518aecc",
        "orderId": "bb210129-f1d5-4942-8d0a-b144e518aecd",
        "totalAmount": 120.00,
        "paymentMethod": "CASH",
        "paymentStatus": "PAID",
        "items": [
          {
            "productId": "b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234",
            "productName": "Pizza Portuguesa",
            "quantity": 2,
            "unitPrice": 60.00
          }
        ]
      }
      """;

    consumer.on(json);

    var commandCaptor = ArgumentCaptor.forClass(CreateTicketCommand.class);
    verify(kitchenService).createTicketFromOrder(commandCaptor.capture());

    assertThat(commandCaptor.getValue().items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.productName()).isEqualTo("Pizza Portuguesa");
              assertThat(item.quantity()).isEqualTo(2);
            });
  }
}
