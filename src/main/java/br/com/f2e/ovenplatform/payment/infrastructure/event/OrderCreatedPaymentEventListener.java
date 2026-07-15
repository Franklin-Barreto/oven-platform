package br.com.f2e.ovenplatform.payment.infrastructure.event;

import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.application.RegisterPaymentCommand;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedPaymentEventListener {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(OrderCreatedPaymentEventListener.class);

  private final PaymentService paymentService;

  public OrderCreatedPaymentEventListener(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @ApplicationModuleListener(id = "payment-order-created-listener")
  public void on(OrderCreatedEvent event) {
    var command =
        new RegisterPaymentCommand(
            event.tenantId(),
            event.orderId(),
            event.totalAmount(),
            PaymentMethod.valueOf(event.paymentMethod().name()),
            PaymentStatus.valueOf(event.paymentStatus().name()));

    try {
      paymentService.registerPaymentFromOrder(command);
    } catch (DataIntegrityViolationException _) {
      LOGGER.info(
          "Ignoring duplicated order.created event for tenantId={} orderId={}",
          command.tenantId(),
          command.orderId());
    }
  }
}
