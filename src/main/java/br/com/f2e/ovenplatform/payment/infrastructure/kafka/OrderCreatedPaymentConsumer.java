package br.com.f2e.ovenplatform.payment.infrastructure.kafka;

import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.application.RegisterPaymentCommand;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderPaymentStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedPaymentConsumer {

  private final PaymentService paymentService;
  private final ObjectMapper mapper;

  public OrderCreatedPaymentConsumer(PaymentService paymentService, ObjectMapper mapper) {
    this.paymentService = paymentService;
    this.mapper = mapper;
  }

  @KafkaListener(
      topics = "${oven.kafka.topics.orders}",
      groupId = "${oven.kafka.consumer-groups.payment}")
  public void on(String payload) {
    var orderCreatedPayload = mapper.readValue(payload, OrderCreatedPayload.class);
    paymentService.registerPaymentFromOrder(
        new RegisterPaymentCommand(
            orderCreatedPayload.tenantId(),
            orderCreatedPayload.orderId(),
            orderCreatedPayload.totalAmount(),
            toPaymentMethod(orderCreatedPayload.paymentMethod().name()),
            toPaymentStatus(orderCreatedPayload.paymentStatus())));
  }

  private static PaymentStatus toPaymentStatus(OrderPaymentStatus status) {
    return PaymentStatus.valueOf(status.name());
  }

  private static PaymentMethod toPaymentMethod(String method) {
    return PaymentMethod.valueOf(method);
  }
}
