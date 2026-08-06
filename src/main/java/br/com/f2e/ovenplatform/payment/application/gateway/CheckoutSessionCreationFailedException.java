package br.com.f2e.ovenplatform.payment.application.gateway;

public class CheckoutSessionCreationFailedException extends RuntimeException {

  public CheckoutSessionCreationFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
