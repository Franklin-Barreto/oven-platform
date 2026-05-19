package br.com.f2e.ovenplatform.payment.infrastructure.persistence;

import br.com.f2e.ovenplatform.payment.application.OrderPaymentResponse;
import br.com.f2e.ovenplatform.payment.domain.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataPaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

  @Query(
      """
        SELECT new br.com.f2e.ovenplatform.payment.application.OrderPaymentResponse(
                p.orderId,
                p.method,
                p.status,
                p.paidAt
                )
        FROM Payment p
        WHERE p.tenantId = :tenantId
        AND p.orderId in :orderIds
        """)
  List<OrderPaymentResponse> findByTenantIdAndOrderIdIn(UUID tenantId, List<UUID> orderIds);
}
