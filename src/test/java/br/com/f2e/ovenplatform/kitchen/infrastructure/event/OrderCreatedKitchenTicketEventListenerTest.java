package br.com.f2e.ovenplatform.kitchen.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class OrderCreatedKitchenTicketEventListenerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final UUID SECOND_PRODUCT_ID =
      UUID.fromString("c6c7d4e3-4f70-46d6-9b5c-9e7e9b0d5678");

  @Mock private KitchenService kitchenService;

  private OrderCreatedKitchenTicketEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new OrderCreatedKitchenTicketEventListener(kitchenService);
  }

  @Test
  void shouldCreateKitchenTicketFromCanonicalOrderCreatedEvent() {
    listener.on(orderCreatedEvent());

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
              assertThat(firstItem.quantity()).isEqualTo(2);
            },
            secondItem -> {
              assertThat(secondItem.productId()).isEqualTo(SECOND_PRODUCT_ID);
              assertThat(secondItem.productName()).isEqualTo("Pizza Calabresa");
              assertThat(secondItem.quantity()).isOne();
            });
  }

  @Test
  void shouldTreatUniqueConstraintViolationAsAnIdempotentRedelivery() {
    doThrow(new DataIntegrityViolationException("Duplicated tenant and order"))
        .when(kitchenService)
        .createTicketFromOrder(any());

    assertThatCode(() -> listener.on(orderCreatedEvent())).doesNotThrowAnyException();
  }

  private OrderCreatedEvent orderCreatedEvent() {
    return new OrderCreatedEvent(
        TENANT_ID,
        ORDER_ID,
        PaymentMethod.CASH,
        PaymentStatus.PAID,
        new BigDecimal("180.00"),
        List.of(
            new OrderPlacedItem(PRODUCT_ID, "Pizza Portuguesa", 2, new BigDecimal("60.00")),
            new OrderPlacedItem(SECOND_PRODUCT_ID, "Pizza Calabresa", 1, new BigDecimal("60.00"))));
  }
}
