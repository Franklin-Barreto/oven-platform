package br.com.f2e.ovenplatform.identity.domain.exception;

public class TenantMembershipInactiveException extends RuntimeException {
  public TenantMembershipInactiveException() {
    super("Tenant membership is inactive.");
  }
}
