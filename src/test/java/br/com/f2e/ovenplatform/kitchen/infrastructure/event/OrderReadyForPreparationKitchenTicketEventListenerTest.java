package br.com.f2e.ovenplatform.kitchen.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.orders.application.event.OrderReadyForPreparationEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class OrderReadyForPreparationKitchenTicketEventListenerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final UUID SECOND_PRODUCT_ID =
      UUID.fromString("c6c7d4e3-4f70-46d6-9b5c-9e7e9b0d5678");
  private static final UUID VARIANT_ID = UUID.fromString("d6c7d4e3-4f70-46d6-9b5c-9e7e9b0d5678");

  @Mock private KitchenService kitchenService;

  @InjectMocks private OrderReadyForPreparationKitchenTicketEventListener listener;

  @Test
  void shouldCreateKitchenTicketWhenOrderIsReadyForPreparation() {
    listener.on(orderReadyForPreparationEvent());

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
              assertThat(firstItem.productName()).isEqualTo("Pizza Portuguesa");
              assertThat(firstItem.variantId()).isEqualTo(VARIANT_ID);
              assertThat(firstItem.variantName()).isEqualTo("Grande");
              assertThat(firstItem.quantity()).isEqualTo(2);
            },
            secondItem -> {
              assertThat(secondItem.productId()).isEqualTo(SECOND_PRODUCT_ID);
              assertThat(secondItem.productName()).isEqualTo("Pizza Calabresa");
              assertThat(secondItem.variantId()).isNull();
              assertThat(secondItem.variantName()).isNull();
              assertThat(secondItem.quantity()).isOne();
            });
  }

  @Test
  void shouldTreatUniqueConstraintViolationAsAnIdempotentRedelivery() {
    doThrow(new DataIntegrityViolationException("Duplicated tenant and order"))
        .when(kitchenService)
        .createTicketFromOrder(any());

    assertThatCode(() -> listener.on(orderReadyForPreparationEvent())).doesNotThrowAnyException();
  }

  private OrderReadyForPreparationEvent orderReadyForPreparationEvent() {
    return new OrderReadyForPreparationEvent(
        TENANT_ID,
        ORDER_ID,
        Instant.parse("2026-08-03T12:00:00Z"),
        List.of(
            new OrderPlacedItem(PRODUCT_ID, "Pizza Portuguesa", VARIANT_ID, "Grande", 2, null),
            new OrderPlacedItem(SECOND_PRODUCT_ID, "Pizza Calabresa", 1, null)));
  }
}
