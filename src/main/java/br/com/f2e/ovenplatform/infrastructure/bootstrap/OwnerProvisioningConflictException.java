package br.com.f2e.ovenplatform.infrastructure.bootstrap;

public class OwnerProvisioningConflictException extends IllegalStateException {

  public OwnerProvisioningConflictException(String message) {
    super(message);
  }
}
