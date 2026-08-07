package br.com.f2e.ovenplatform.payment.application.checkout;

import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;

public class UnsupportedCheckoutPaymentMethodException extends RuntimeException {

  public UnsupportedCheckoutPaymentMethodException(PaymentMethod paymentMethod) {
    super(
        "Checkout sessions are only supported for CARD payments, but received %s."
            .formatted(paymentMethod));
  }
}
