package br.com.f2e.ovenplatform.payment.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

  @Version
  @Column(nullable = false)
  private long version;

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private UUID orderId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false)
  private PaymentMethod method;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PaymentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_mode", nullable = false)
  private PaymentProcessingMode processingMode;

  @Column(name = "paid_at")
  private Instant paidAt;

  protected Payment() {}

  private Payment(
      UUID tenantId,
      UUID orderId,
      BigDecimal amount,
      PaymentMethod method,
      PaymentStatus status,
      PaymentProcessingMode processingMode,
      Instant paidAt) {
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.orderId = requireNotNull(orderId, "orderId");
    this.amount = requirePositive(amount, "amount");
    this.method = requireNotNull(method, "paymentMethod");
    this.status = requireNotNull(status, "paymentStatus");
    this.processingMode = requireNotNull(processingMode, "processingMode");
    this.paidAt = paidAt;
  }

  public static Payment paid(
      UUID tenantId,
      UUID orderId,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      PaymentProcessingMode processingMode,
      Instant paidAt) {
    requireNotNull(paidAt, "paidAt");
    return new Payment(
        tenantId, orderId, amount, paymentMethod, PaymentStatus.PAID, processingMode, paidAt);
  }

  public static Payment pending(
      UUID tenantId,
      UUID orderId,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      PaymentProcessingMode processingMode) {
    return new Payment(
        tenantId, orderId, amount, paymentMethod, PaymentStatus.PENDING, processingMode, null);
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public long getVersion() {
    return version;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public PaymentMethod getMethod() {
    return method;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public PaymentProcessingMode getProcessingMode() {
    return processingMode;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public boolean markAsPaid(Instant paidAt) {
    if (status == PaymentStatus.PAID) {
      return false;
    }
    status = PaymentStatus.PAID;
    this.paidAt = requireNotNull(paidAt, "paidAt");
    return true;
  }

  public ExternalPaymentAttempt createExternalAttempt(PaymentProvider provider) {
    requireEligibleForExternalAttempt();

    return ExternalPaymentAttempt.createAttempt(tenantId, getId(), provider, amount);
  }

  private void requireEligibleForExternalAttempt() {
    if (status != PaymentStatus.PENDING) {
      throw new IllegalStateException(
          "Only pending payments can create external payment attempts.");
    }

    if (processingMode != PaymentProcessingMode.GATEWAY) {
      throw new IllegalStateException(
          "Only gateway-processed payments can create external payment attempts.");
    }
  }
}
