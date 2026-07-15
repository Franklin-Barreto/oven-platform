package br.com.f2e.ovenplatform.payment.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.application.RegisterPaymentCommand;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
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
class OrderCreatedPaymentEventListenerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("120.00");

  @Mock private PaymentService paymentService;

  private OrderCreatedPaymentEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new OrderCreatedPaymentEventListener(paymentService);
  }

  @Test
  void shouldRegisterPaymentFromCanonicalOrderCreatedEvent() {
    listener.on(orderCreatedEvent());

    var commandCaptor = ArgumentCaptor.forClass(RegisterPaymentCommand.class);
    verify(paymentService).registerPaymentFromOrder(commandCaptor.capture());

    assertThat(commandCaptor.getValue())
        .isEqualTo(
            new RegisterPaymentCommand(
                TENANT_ID, ORDER_ID, TOTAL_AMOUNT, PaymentMethod.CASH, PaymentStatus.PAID));
  }

  @Test
  void shouldTreatUniqueConstraintViolationAsAnIdempotentRedelivery() {
    doThrow(new DataIntegrityViolationException("Duplicated tenant and order"))
        .when(paymentService)
        .registerPaymentFromOrder(any());

    assertThatCode(() -> listener.on(orderCreatedEvent())).doesNotThrowAnyException();
  }

  private OrderCreatedEvent orderCreatedEvent() {
    return new OrderCreatedEvent(
        TENANT_ID,
        ORDER_ID,
        br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod.CASH,
        br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus.PAID,
        TOTAL_AMOUNT,
        List.of(new OrderPlacedItem(PRODUCT_ID, "Pizza Portuguesa", 2, new BigDecimal("60.00"))));
  }
}
