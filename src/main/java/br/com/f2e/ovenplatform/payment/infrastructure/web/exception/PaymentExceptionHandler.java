package br.com.f2e.ovenplatform.payment.infrastructure.web.exception;

import br.com.f2e.ovenplatform.payment.application.checkout.UnsupportedCheckoutPaymentMethodException;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationFailedException;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationOutcomeUnknownException;
import br.com.f2e.ovenplatform.payment.domain.exception.ExternalPaymentAttemptNotAllowedException;
import br.com.f2e.ovenplatform.payment.domain.exception.InvalidExternalPaymentAttemptStatusTransitionException;
import br.com.f2e.ovenplatform.payment.infrastructure.web.PaymentController;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {PaymentController.class})
public class PaymentExceptionHandler {

  private final ApiErrorResponseFactory factory;

  public PaymentExceptionHandler(ApiErrorResponseFactory factory) {
    this.factory = factory;
  }

  @ExceptionHandler(CheckoutSessionCreationOutcomeUnknownException.class)
  public ResponseEntity<ApiErrorResponse> handleCheckoutSessionCreationOutcomeUnknownException(
      CheckoutSessionCreationOutcomeUnknownException exception, HttpServletRequest request) {
    return factory.create(
        HttpStatus.SERVICE_UNAVAILABLE,
        ApiErrorCodes.PAYMENT_GATEWAY_OUTCOME_UNKNOWN,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(CheckoutSessionCreationFailedException.class)
  public ResponseEntity<ApiErrorResponse> handleCheckoutSessionCreationFailedException(
      CheckoutSessionCreationFailedException exception, HttpServletRequest request) {
    return factory.create(
        HttpStatus.BAD_GATEWAY,
        ApiErrorCodes.PAYMENT_GATEWAY_FAILURE,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(InvalidExternalPaymentAttemptStatusTransitionException.class)
  public ResponseEntity<ApiErrorResponse>
      handleInvalidExternalPaymentAttemptStatusTransitionException(
          InvalidExternalPaymentAttemptStatusTransitionException exception,
          HttpServletRequest request) {
    return factory.create(
        HttpStatus.CONFLICT,
        ApiErrorCodes.INVALID_EXTERNAL_PAYMENT_ATTEMPT_STATUS_TRANSITION,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(ExternalPaymentAttemptNotAllowedException.class)
  public ResponseEntity<ApiErrorResponse> externalPaymentAttemptNotAllowed(
      ExternalPaymentAttemptNotAllowedException exception, HttpServletRequest request) {
    return factory.create(
        HttpStatus.CONFLICT,
        ApiErrorCodes.EXTERNAL_PAYMENT_ATTEMPT_NOT_ALLOWED,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(UnsupportedCheckoutPaymentMethodException.class)
  public ResponseEntity<ApiErrorResponse> handleUnsupportedCheckoutPaymentMethodException(
      UnsupportedCheckoutPaymentMethodException exception, HttpServletRequest request) {
    return factory.create(
        HttpStatus.CONFLICT,
        ApiErrorCodes.EXTERNAL_PAYMENT_METHOD_NOT_ALLOWED,
        exception.getMessage(),
        request);
  }
}
