package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final Clock clock;

  public PaymentService(PaymentRepository paymentRepository, Clock clock) {
    this.paymentRepository = paymentRepository;
    this.clock = clock;
  }

  @Transactional
  public void registerPaymentFromOrder(RegisterPaymentCommand paymentCommand) {
    var payment =
        switch (paymentCommand.paymentStatus()) {
          case PAID ->
              Payment.paid(
                  paymentCommand.tenantId(),
                  paymentCommand.orderId(),
                  paymentCommand.amount(),
                  paymentCommand.paymentMethod(),
                  clock.instant());
          case PENDING ->
              Payment.pending(
                  paymentCommand.tenantId(),
                  paymentCommand.orderId(),
                  paymentCommand.amount(),
                  paymentCommand.paymentMethod());
        };
    paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public Payment findByTenantIdAndOrderId(UUID tenantId, UUID orderId) {
    return getByTenantIdAndOrderId(tenantId, orderId);
  }

  @Transactional
  public void markAsPaid(UUID tenantId, UUID orderId) {
    var payment = getByTenantIdAndOrderId(tenantId, orderId);
    payment.markAsPaid(clock.instant());
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
  public List<OrderPaymentResponse> findByTenantIdAndOrderIdIn(UUID tenantId, List<UUID> orderIds) {
    return paymentRepository.findByTenantIdAndOrderIdIn(tenantId, orderIds);
  }
}
