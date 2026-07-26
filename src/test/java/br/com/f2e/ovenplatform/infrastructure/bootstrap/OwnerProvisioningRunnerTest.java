package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class OwnerProvisioningRunnerTest {

  @Mock private OwnerProvisioningService provisioningService;

  @Test
  void shouldProvisionOwnerFromConfigurationProperties() {
    var properties =
        new OwnerProvisioningProperties(
            "Don Corleone Pizzeria", "owner@oven.local", "OwnerPass123!");
    var command =
        new OwnerProvisioningCommand(
            properties.tenantName(), properties.email(), properties.password());
    var result =
        new OwnerProvisioningResult(
            UUID.randomUUID(), UUID.randomUUID(), OwnerProvisioningResult.Outcome.PROVISIONED);
    var runner = new OwnerProvisioningRunner(provisioningService, properties);

    when(provisioningService.provision(command)).thenReturn(result);

    runner.run(new DefaultApplicationArguments());

    verify(provisioningService).provision(command);
  }
}
