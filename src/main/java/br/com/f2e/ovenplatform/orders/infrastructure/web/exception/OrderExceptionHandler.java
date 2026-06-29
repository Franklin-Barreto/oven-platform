package br.com.f2e.ovenplatform.orders.infrastructure.web.exception;

import br.com.f2e.ovenplatform.orders.application.ProductNotAvailableForOrderingException;
import br.com.f2e.ovenplatform.orders.domain.exception.InvalidOrderStatusTransitionException;
import br.com.f2e.ovenplatform.orders.infrastructure.web.OrderController;
import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderExceptionHandler {

  private final TraceContext traceContext;

  public OrderExceptionHandler(TraceContext traceContext) {
    this.traceContext = traceContext;
  }

  @ExceptionHandler(InvalidOrderStatusTransitionException.class)
  public ResponseEntity<ApiErrorResponse> invalidOrderStatusTransition(
      InvalidOrderStatusTransitionException exception, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT,
        ApiErrorCodes.INVALID_ORDER_STATUS_TRANSITION,
        resolveTraceIdForErrorResponse(traceContext),
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(ProductNotAvailableForOrderingException.class)
  public ResponseEntity<ApiErrorResponse> productNotAvailableForOrdering(
      ProductNotAvailableForOrderingException exception, HttpServletRequest request) {
    return error(
        HttpStatus.NOT_FOUND,
        ApiErrorCodes.RESOURCE_NOT_FOUND,
        resolveTraceIdForErrorResponse(traceContext),
        exception.getMessage(),
        request);
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status, String code, String traceId, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(status, code, traceId, message, request.getRequestURI()));
  }

  private String resolveTraceIdForErrorResponse(TraceContext traceContext) {
    return traceContext.findTraceId().orElse(UUID.randomUUID().toString());
  }
}
