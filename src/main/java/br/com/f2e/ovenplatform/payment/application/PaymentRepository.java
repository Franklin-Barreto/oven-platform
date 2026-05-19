package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

  Payment save(Payment payment);

  Optional<Payment> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

  List<OrderPaymentResponse> findByTenantIdAndOrderIdIn(UUID tenantId, List<UUID> orderIds);
}
