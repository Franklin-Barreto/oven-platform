package br.com.f2e.ovenplatform.identity.application.team;

public class TenantTeamManagementDeniedException extends RuntimeException {

  public TenantTeamManagementDeniedException() {
    super("Actor cannot manage the target tenant membership");
  }
}
