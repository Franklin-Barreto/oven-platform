package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("bootstrap-owner")
@EnableConfigurationProperties(OwnerProvisioningProperties.class)
public class OwnerProvisioningRunner implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(OwnerProvisioningRunner.class);

  private final OwnerProvisioningService provisioningService;
  private final OwnerProvisioningProperties properties;

  public OwnerProvisioningRunner(
      OwnerProvisioningService provisioningService, OwnerProvisioningProperties properties) {
    this.provisioningService = provisioningService;
    this.properties = properties;
  }

  @Override
  public void run(@NonNull ApplicationArguments args) {
    LOGGER.info(
        "Starting initial OWNER provisioning tenantName={} ownerEmail={}",
        properties.tenantName(),
        properties.email());

    var result =
        provisioningService.provision(
            new OwnerProvisioningCommand(
                properties.tenantName(), properties.email(), properties.password()));

    LOGGER.info(
        "Initial OWNER provisioning finished outcome={} tenantId={} userId={} ownerEmail={}",
        result.outcome(),
        result.tenantId(),
        result.userId(),
        properties.email());
  }
}
