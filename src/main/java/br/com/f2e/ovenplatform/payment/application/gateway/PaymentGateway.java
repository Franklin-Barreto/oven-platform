package br.com.f2e.ovenplatform.payment.application.gateway;

public interface PaymentGateway {

  CreatedCheckoutSession createCheckoutSession(CheckoutSessionSpec spec);
}
