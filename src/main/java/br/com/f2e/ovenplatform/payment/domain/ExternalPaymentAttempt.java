package br.com.f2e.ovenplatform.payment.domain;

import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.CREATED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.EXPIRED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.FAILED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.PENDING;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.SUCCEEDED;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireSize;

import br.com.f2e.ovenplatform.payment.domain.exception.InvalidExternalPaymentAttemptStatusTransitionException;
import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_payment_attempts")
public class ExternalPaymentAttempt extends BaseEntity {

  private static final String INITIAL_CURRENCY = "BRL";

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "payment_id", nullable = false)
  private UUID paymentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 60)
  private PaymentProvider provider;

  @Column(name = "provider_reference", length = 150)
  private String providerReference;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "redirect_url", length = 2048)
  private String redirectUrl;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ExternalPaymentAttemptStatus status;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  protected ExternalPaymentAttempt() {}

  private ExternalPaymentAttempt(
      UUID tenantId, UUID paymentId, PaymentProvider provider, BigDecimal amount) {

    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.paymentId = requireNotNull(paymentId, "paymentId");
    this.provider = requireNotNull(provider, "provider");
    this.amount = requirePositive(amount, "amount");
    this.currency = INITIAL_CURRENCY;
    this.status = CREATED;
  }

  public static ExternalPaymentAttempt createAttempt(
      UUID tenantId, UUID paymentId, PaymentProvider provider, BigDecimal amount) {
    return new ExternalPaymentAttempt(tenantId, paymentId, provider, amount);
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getPaymentId() {
    return paymentId;
  }

  public PaymentProvider getProvider() {
    return provider;
  }

  public String getProviderReference() {
    return providerReference;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public URI getRedirectUrl() {
    return redirectUrl == null ? null : URI.create(redirectUrl);
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public ExternalPaymentAttemptStatus getStatus() {
    return status;
  }

  public boolean registerCheckout(String providerReference, URI redirectUrl, Instant expiresAt) {

    var validatedProviderReference = requireSize(providerReference, "providerReference", 1, 150);
    var validatedRedirectUrl = requireNotNull(redirectUrl, "redirectUrl");
    var validatedExpiresAt = requireNotNull(expiresAt, "expiresAt");

    var changed = transitionTo(PENDING);

    if (changed) {
      this.providerReference = validatedProviderReference;
      this.redirectUrl = validatedRedirectUrl.toString();
      this.expiresAt = validatedExpiresAt;
    }

    return changed;
  }

  public boolean markAsFailed(Instant completedAt) {
    return completeAs(FAILED, completedAt);
  }

  public boolean markAsExpired(Instant completedAt) {
    return completeAs(EXPIRED, completedAt);
  }

  public boolean markAsSucceeded(Instant completedAt) {
    return completeAs(SUCCEEDED, completedAt);
  }

  public boolean isReusableAt(Instant occurredAt) {
    requireNotNull(occurredAt, "occurredAt");

    return switch (status) {
      case CREATED -> true;
      case PENDING -> expiresAt != null && expiresAt.isAfter(occurredAt);
      case SUCCEEDED, FAILED, EXPIRED -> false;
    };
  }

  public boolean isExpiredAt(Instant occurredAt) {
    requireNotNull(occurredAt, "occurredAt");

    return status == PENDING && expiresAt != null && !expiresAt.isAfter(occurredAt);
  }

  private boolean completeAs(ExternalPaymentAttemptStatus target, Instant completedAt) {

    var validatedCompletedAt = requireNotNull(completedAt, "completedAt");

    var changed = transitionTo(target);

    if (changed) {
      this.completedAt = validatedCompletedAt;
    }

    return changed;
  }

  private boolean transitionTo(ExternalPaymentAttemptStatus target) {
    if (status == target) {
      return false;
    }

    if (!status.canTransitionTo(target)) {
      throw new InvalidExternalPaymentAttemptStatusTransitionException(status, target);
    }

    status = target;
    return true;
  }
}
