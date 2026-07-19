package br.com.f2e.ovenplatform.orders.infrastructure.web.exception;

import br.com.f2e.ovenplatform.orders.application.ProductNotAvailableForOrderingException;
import br.com.f2e.ovenplatform.orders.domain.exception.InvalidOrderStatusTransitionException;
import br.com.f2e.ovenplatform.orders.infrastructure.web.OrderController;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderExceptionHandler {

  private final ApiErrorResponseFactory responseFactory;

  public OrderExceptionHandler(ApiErrorResponseFactory responseFactory) {
    this.responseFactory = responseFactory;
  }

  @ExceptionHandler(InvalidOrderStatusTransitionException.class)
  public ResponseEntity<ApiErrorResponse> invalidOrderStatusTransition(
      InvalidOrderStatusTransitionException exception, HttpServletRequest request) {
    return responseFactory.create(
        HttpStatus.CONFLICT,
        ApiErrorCodes.INVALID_ORDER_STATUS_TRANSITION,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(ProductNotAvailableForOrderingException.class)
  public ResponseEntity<ApiErrorResponse> productNotAvailableForOrdering(
      ProductNotAvailableForOrderingException exception, HttpServletRequest request) {
    return responseFactory.create(
        HttpStatus.NOT_FOUND, ApiErrorCodes.RESOURCE_NOT_FOUND, exception.getMessage(), request);
  }
}
