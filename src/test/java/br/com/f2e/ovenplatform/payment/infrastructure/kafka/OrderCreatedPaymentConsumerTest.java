package br.com.f2e.ovenplatform.payment.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.application.RegisterPaymentCommand;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedItemPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedPayload;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreatedPaymentConsumerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("120.00");

  @Mock private PaymentService paymentService;

  private OrderCreatedPaymentConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new OrderCreatedPaymentConsumer(paymentService, JsonUtils.getObjectMapper());
  }

  @Test
  void shouldRegisterPaymentFromOrderCreatedPayload() {
    var payload =
        new OrderCreatedPayload(
            TENANT_ID,
            ORDER_ID,
            TOTAL_AMOUNT,
            PaymentMethod.CASH,
            PaymentStatus.PAID,
            List.of(
                new OrderCreatedItemPayload(
                    PRODUCT_ID, "Pizza Portuguesa", 2, new BigDecimal("60.00"))));
    var json = JsonUtils.toJson(payload);

    consumer.on(json);

    var commandCaptor = ArgumentCaptor.forClass(RegisterPaymentCommand.class);
    verify(paymentService).registerPaymentFromOrder(commandCaptor.capture());

    var command = commandCaptor.getValue();

    assertThat(command.tenantId()).isEqualTo(TENANT_ID);
    assertThat(command.orderId()).isEqualTo(ORDER_ID);
    assertThat(command.amount()).isEqualByComparingTo(TOTAL_AMOUNT);
    assertThat(command.paymentMethod())
        .isEqualTo(br.com.f2e.ovenplatform.payment.domain.PaymentMethod.CASH);
    assertThat(command.paymentStatus())
        .isEqualTo(br.com.f2e.ovenplatform.payment.domain.PaymentStatus.PAID);
  }
}
