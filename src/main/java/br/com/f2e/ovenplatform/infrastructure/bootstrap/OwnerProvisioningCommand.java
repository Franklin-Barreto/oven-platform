package br.com.f2e.ovenplatform.infrastructure.bootstrap;

public record OwnerProvisioningCommand(String tenantName, String email, String password) {}
