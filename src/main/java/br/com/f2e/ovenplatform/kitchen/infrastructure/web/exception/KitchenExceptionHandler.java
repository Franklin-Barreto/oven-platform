package br.com.f2e.ovenplatform.kitchen.infrastructure.web.exception;

import br.com.f2e.ovenplatform.kitchen.domain.exception.InvalidTicketStatusTransitionException;
import br.com.f2e.ovenplatform.kitchen.infrastructure.web.KitchenTicketController;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = KitchenTicketController.class)
public class KitchenExceptionHandler {

  private final ApiErrorResponseFactory responseFactory;

  public KitchenExceptionHandler(ApiErrorResponseFactory responseFactory) {
    this.responseFactory = responseFactory;
  }

  @ExceptionHandler(InvalidTicketStatusTransitionException.class)
  public ResponseEntity<ApiErrorResponse> invalidTicketStatusTransitionException(
      InvalidTicketStatusTransitionException exception, HttpServletRequest request) {
    return responseFactory.create(
        HttpStatus.CONFLICT,
        ApiErrorCodes.INVALID_TICKET_STATUS_TRANSITION,
        exception.getMessage(),
        request);
  }
}
