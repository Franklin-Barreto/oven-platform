package br.com.f2e.ovenplatform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  ApplicationModules modules = ApplicationModules.of(OvenPlatformApplication.class);

  @Test
  void verifiesModularStructure() {
    modules.verify();
  }
}
