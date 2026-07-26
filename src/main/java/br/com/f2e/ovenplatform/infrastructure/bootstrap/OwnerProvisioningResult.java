package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import java.util.UUID;

public record OwnerProvisioningResult(UUID tenantId, UUID userId, Outcome outcome) {

  public enum Outcome {
    PROVISIONED,
    ALREADY_PROVISIONED
  }
}
