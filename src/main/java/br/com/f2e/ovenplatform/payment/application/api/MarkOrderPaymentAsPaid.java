package br.com.f2e.ovenplatform.payment.application.api;

import java.util.UUID;

public interface MarkOrderPaymentAsPaid {
  void markAsPaid(UUID tenantId, UUID orderId);
}
