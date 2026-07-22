package br.com.f2e.ovenplatform.identity.infrastructure.web.exception;

import br.com.f2e.ovenplatform.identity.application.team.TenantTeamManagementDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import br.com.f2e.ovenplatform.identity.infrastructure.web.AuthenticationController;
import br.com.f2e.ovenplatform.identity.infrastructure.web.IdentityController;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = {IdentityController.class, AuthenticationController.class})
public class IdentityExceptionHandler {

  private final ApiErrorResponseFactory responseFactory;

  public IdentityExceptionHandler(ApiErrorResponseFactory responseFactory) {
    this.responseFactory = responseFactory;
  }

  @ExceptionHandler(TenantMembershipInactiveException.class)
  public ResponseEntity<ApiErrorResponse> tenantMembershipInactiveException(
      TenantMembershipInactiveException exception, HttpServletRequest request) {
    return responseFactory.create(
        HttpStatus.FORBIDDEN,
        ApiErrorCodes.INACTIVE_TENANT_MEMBERSHIP,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(TenantAccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> tenantAccessDeniedHandler(
      TenantAccessDeniedException exception, HttpServletRequest request) {
    return responseFactory.create(
        HttpStatus.FORBIDDEN, ApiErrorCodes.TENANT_ACCESS_DENIED, exception.getMessage(), request);
  }

  @ExceptionHandler(TenantTeamManagementDeniedException.class)
  public ResponseEntity<ApiErrorResponse> tenantTeamManagementDeniedHandler(
      TenantTeamManagementDeniedException exception, HttpServletRequest request) {
    return responseFactory.create(
        HttpStatus.FORBIDDEN,
        ApiErrorCodes.TENANT_TEAM_MANAGEMENT_DENIED,
        exception.getMessage(),
        request);
  }
}
