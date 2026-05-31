package br.com.f2e.ovenplatform.identity.infrastructure.web.exception;

import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import br.com.f2e.ovenplatform.identity.infrastructure.web.AuthenticationController;
import br.com.f2e.ovenplatform.identity.infrastructure.web.IdentityController;
import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = {IdentityController.class, AuthenticationController.class})
public class IdentityExceptionHandler {

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "TraceContext is a Spring-managed dependency used to read the current request trace id.")
  private final TraceContext traceContext;

  public IdentityExceptionHandler(TraceContext traceContext) {
    this.traceContext = traceContext;
  }

  @ExceptionHandler(TenantMembershipInactiveException.class)
  public ResponseEntity<ApiErrorResponse> tenantMembershipInactiveException(
      TenantMembershipInactiveException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ApiErrorResponse.of(
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.INACTIVE_TENANT_MEMBERSHIP,
                resolveTraceIdForErrorResponse(traceContext),
                exception.getMessage(),
                request.getRequestURI()));
  }

  private String resolveTraceIdForErrorResponse(TraceContext traceContext) {
    return traceContext.findTraceId().orElse(UUID.randomUUID().toString());
  }
}
