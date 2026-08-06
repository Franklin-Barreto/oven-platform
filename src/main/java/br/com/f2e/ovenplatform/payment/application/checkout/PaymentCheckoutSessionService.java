package br.com.f2e.ovenplatform.payment.application.checkout;

import br.com.f2e.ovenplatform.payment.application.CreateExternalPaymentAttemptCommand;
import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptResult;
import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptService;
import br.com.f2e.ovenplatform.payment.application.PaymentRepository;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionSpec;
import br.com.f2e.ovenplatform.payment.application.gateway.PaymentGateway;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.UUID;

public class PaymentCheckoutSessionService {

  private final PaymentRepository paymentRepository;
  private final ExternalPaymentAttemptService attemptService;
  private final PaymentGateway paymentGateway;

  public PaymentCheckoutSessionService(
      PaymentRepository paymentRepository,
      ExternalPaymentAttemptService attemptService,
      PaymentGateway paymentGateway) {
    this.paymentRepository = paymentRepository;
    this.attemptService = attemptService;
    this.paymentGateway = paymentGateway;
  }

  public PaymentCheckoutSessionResult createOrReuseCheckoutSession(UUID tenantId, UUID orderId) {
    var payment =
        paymentRepository
            .findByTenantIdAndOrderId(tenantId, orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

    var externalPaymentAttemptResult =
        attemptService.createOrReuseAttempt(
            new CreateExternalPaymentAttemptCommand(
                tenantId, payment.getId(), PaymentProvider.STRIPE));

    return switch (externalPaymentAttemptResult.status()) {
      case PENDING -> checkoutResultFrom(externalPaymentAttemptResult);
      case CREATED ->
          createAndRegisterCheckout(payment.getTenantId(), externalPaymentAttemptResult);
      case SUCCEEDED, FAILED, EXPIRED ->
          throw new IllegalStateException(
              "Unexpected external payment attempt status for checkout creation: %s"
                  .formatted(externalPaymentAttemptResult.status()));
    };
  }

  private PaymentCheckoutSessionResult createAndRegisterCheckout(
      UUID tenantId, ExternalPaymentAttemptResult externalPaymentAttemptResult) {
    var createdCheckoutSession =
        paymentGateway.createCheckoutSession(
            new CheckoutSessionSpec(
                externalPaymentAttemptResult.attemptId(),
                externalPaymentAttemptResult.amount(),
                externalPaymentAttemptResult.currency()));

    var attemptResult =
        attemptService.registerCheckout(
            new RegisterExternalCheckoutCommand(
                tenantId,
                externalPaymentAttemptResult.attemptId(),
                createdCheckoutSession.providerReference(),
                createdCheckoutSession.checkoutUrl(),
                createdCheckoutSession.expiresAt()));
    return new PaymentCheckoutSessionResult(
        attemptResult.attemptId(), attemptResult.redirectUrl(), attemptResult.expiresAt());
  }

  private PaymentCheckoutSessionResult checkoutResultFrom(
      ExternalPaymentAttemptResult externalPaymentAttemptResult) {
    return new PaymentCheckoutSessionResult(
        externalPaymentAttemptResult.attemptId(),
        externalPaymentAttemptResult.redirectUrl(),
        externalPaymentAttemptResult.expiresAt());
  }
}
