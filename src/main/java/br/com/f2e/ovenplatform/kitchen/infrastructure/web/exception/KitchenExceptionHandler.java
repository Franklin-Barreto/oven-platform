package br.com.f2e.ovenplatform.kitchen.infrastructure.web.exception;

import br.com.f2e.ovenplatform.kitchen.domain.exception.InvalidTicketStatusTransitionException;
import br.com.f2e.ovenplatform.kitchen.infrastructure.web.KitchenTicketController;
import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = KitchenTicketController.class)
public class KitchenExceptionHandler {

  private final TraceContext traceContext;

  public KitchenExceptionHandler(TraceContext traceContext) {
    this.traceContext = traceContext;
  }

  @ExceptionHandler(InvalidTicketStatusTransitionException.class)
  public ResponseEntity<ApiErrorResponse> invalidTicketStatusTransitionException(
      InvalidTicketStatusTransitionException exception, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT,
        ApiErrorCodes.INVALID_TICKET_STATUS_TRANSITION,
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
