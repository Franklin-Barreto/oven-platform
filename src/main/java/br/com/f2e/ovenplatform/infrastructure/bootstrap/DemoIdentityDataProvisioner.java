package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local-demo")
public class DemoIdentityDataProvisioner implements ApplicationRunner {

  private final DemoIdentityProvisioningService provisioningService;

  public DemoIdentityDataProvisioner(DemoIdentityProvisioningService provisioningService) {
    this.provisioningService = provisioningService;
  }

  @Override
  public void run(@NonNull ApplicationArguments args) {
    provisioningService.provision();
  }
}
