package br.com.f2e.ovenplatform.payment.infrastructure.persistence;

import br.com.f2e.ovenplatform.payment.application.OrderPaymentResult;
import br.com.f2e.ovenplatform.payment.application.PaymentRepository;
import br.com.f2e.ovenplatform.payment.domain.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPaymentRepositoryAdapter implements PaymentRepository {

  private final SpringDataPaymentRepository paymentRepository;

  public JpaPaymentRepositoryAdapter(SpringDataPaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Override
  public Payment save(Payment payment) {
    return paymentRepository.save(payment);
  }

  @Override
  public Optional<Payment> findByTenantIdAndOrderId(UUID tenantId, UUID orderId) {
    return paymentRepository.findByTenantIdAndOrderId(tenantId, orderId);
  }

  @Override
  public List<OrderPaymentResult> findByTenantIdAndOrderIdIn(UUID tenantId, List<UUID> orderIds) {
    return paymentRepository.findByTenantIdAndOrderIdIn(tenantId, orderIds);
  }

  @Override
  public Optional<Payment> findByIdAndTenantId(UUID paymentId, UUID tenantId) {
    return paymentRepository.findByIdAndTenantId(paymentId, tenantId);
  }
}
