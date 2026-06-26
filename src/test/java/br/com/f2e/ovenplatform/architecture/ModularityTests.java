package br.com.f2e.ovenplatform.architecture;

import br.com.f2e.ovenplatform.OvenPlatformApplication;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

  ApplicationModules modules =
      ApplicationModules.of(
          OvenPlatformApplication.class,
          DescribedPredicate.not(
              JavaClass.Predicates.resideInAPackage(
                  "br.com.f2e.ovenplatform.infrastructure.bootstrap..")));

  @Test
  void verifiesModularStructure() {
    modules.verify();
  }
}
