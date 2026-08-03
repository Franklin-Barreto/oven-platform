package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.payment.domain.PaymentProcessingMode;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentConfirmedEvent;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final Clock clock;
  private final ApplicationEventPublisher eventPublisher;

  public PaymentService(
      PaymentRepository paymentRepository, Clock clock, ApplicationEventPublisher eventPublisher) {
    this.paymentRepository = paymentRepository;
    this.clock = clock;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public void registerPaymentFromOrder(RegisterPaymentCommand paymentCommand) {
    if (paymentRepository
        .findByTenantIdAndOrderId(paymentCommand.tenantId(), paymentCommand.orderId())
        .isPresent()) {
      return;
    }

    var payment =
        switch (paymentCommand.paymentStatus()) {
          case PAID ->
              Payment.paid(
                  paymentCommand.tenantId(),
                  paymentCommand.orderId(),
                  paymentCommand.amount(),
                  paymentCommand.paymentMethod(),
                  paymentCommand.processingMode(),
                  clock.instant());
          case PENDING ->
              Payment.pending(
                  paymentCommand.tenantId(),
                  paymentCommand.orderId(),
                  paymentCommand.amount(),
                  paymentCommand.paymentMethod(),
                  paymentCommand.processingMode());
        };
    paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public Payment findByTenantIdAndOrderId(UUID tenantId, UUID orderId) {
    return getByTenantIdAndOrderId(tenantId, orderId);
  }

  @Transactional
  public void markManualPaymentAsPaid(UUID tenantId, UUID orderId) {
    var payment = getByTenantIdAndOrderId(tenantId, orderId);
    requireProcessingMode(payment, PaymentProcessingMode.MANUAL);
    markAsPaid(payment);
  }

  @Transactional
  public void confirmGatewayPayment(UUID tenantId, UUID orderId) {
    var payment = getByTenantIdAndOrderId(tenantId, orderId);
    requireProcessingMode(payment, PaymentProcessingMode.GATEWAY);
    markAsPaid(payment);
  }

  private void markAsPaid(Payment payment) {
    var paidAt = clock.instant();
    if (payment.markAsPaid(paidAt)) {
      eventPublisher.publishEvent(
          new PaymentConfirmedEvent(payment.getTenantId(), payment.getOrderId(), paidAt));
    }
  }

  private void requireProcessingMode(Payment payment, PaymentProcessingMode expectedMode) {
    if (payment.getProcessingMode() != expectedMode) {
      throw new IllegalStateException("payment processing mode must be %s".formatted(expectedMode));
    }
  }

  private Payment getByTenantIdAndOrderId(UUID tenantId, UUID orderId) {
    return paymentRepository
        .findByTenantIdAndOrderId(tenantId, orderId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Payment for order id: %s not found".formatted(orderId)));
  }

  @Transactional(readOnly = true)
  public List<OrderPaymentResult> findByTenantIdAndOrderIdIn(UUID tenantId, List<UUID> orderIds) {
    return paymentRepository.findByTenantIdAndOrderIdIn(tenantId, orderIds);
  }
}
