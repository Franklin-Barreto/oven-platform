package br.com.f2e.ovenplatform.identity.domain.exception;

public class TenantAccessDeniedException extends RuntimeException {
  public TenantAccessDeniedException() {
    super("User does not have access to this tenant.");
  }
}
