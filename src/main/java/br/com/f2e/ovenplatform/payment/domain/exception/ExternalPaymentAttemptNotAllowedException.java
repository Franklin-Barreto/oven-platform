package br.com.f2e.ovenplatform.payment.domain.exception;

public class ExternalPaymentAttemptNotAllowedException extends RuntimeException {

  public ExternalPaymentAttemptNotAllowedException(String message) {
    super(message);
  }
}
