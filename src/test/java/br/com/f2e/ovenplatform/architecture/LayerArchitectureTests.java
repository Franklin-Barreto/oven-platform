package br.com.f2e.ovenplatform.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LayerArchitectureTests {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setUp() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("br.com.f2e.ovenplatform");
  }

  @Test
  void domainShouldNotDependOnWeb() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure.web..")
        .check(importedClasses);
  }

  @Test
  void domainShouldNotDependOnApplication() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..application..")
        .check(importedClasses);
  }

  @Test
  void applicationShouldNotDependOnInfrastructure() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..")
        .check(importedClasses);
  }

  @Test
  void webShouldNotDependOnSpringDataRepositories() {
    noClasses()
        .that()
        .resideInAPackage("..infrastructure.web..")
        .should()
        .dependOnClassesThat()
        .areAssignableTo(org.springframework.data.repository.Repository.class)
        .check(importedClasses);
  }

  @Test
  void springDataRepositoriesShouldOnlyBeAccessedByPersistenceAdapters() {
    classes()
        .that()
        .areAssignableTo(org.springframework.data.repository.Repository.class)
        .should()
        .onlyBeAccessed()
        .byClassesThat()
        .resideInAPackage("..infrastructure.persistence..")
        .check(importedClasses);
  }
}
