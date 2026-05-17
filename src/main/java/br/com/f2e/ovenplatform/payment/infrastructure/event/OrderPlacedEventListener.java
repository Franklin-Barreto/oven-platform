package br.com.f2e.ovenplatform.payment.infrastructure.event;

import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentMethod;
import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentStatus;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedEvent;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.application.RegisterPaymentCommand;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedEventListener {

  private final PaymentService paymentService;

  public OrderPlacedEventListener(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @ApplicationModuleListener
  void on(OrderPlacedEvent event) {
    var command =
        new RegisterPaymentCommand(
            event.tenantId(),
            event.orderId(),
            event.totalAmount(),
            toPaymentMethod(event.paymentMethod()),
            toPaymentStatus(event.paymentStatus()));
    paymentService.registerPaymentFromOrder(command);
  }

  private static PaymentStatus toPaymentStatus(OrderPaymentStatus status) {
    return PaymentStatus.valueOf(status.name());
  }

  private static PaymentMethod toPaymentMethod(OrderPaymentMethod method) {
    return PaymentMethod.valueOf(method.name());
  }
}
