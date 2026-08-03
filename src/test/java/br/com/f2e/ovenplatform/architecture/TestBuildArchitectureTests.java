package br.com.f2e.ovenplatform.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

class TestBuildArchitectureTests {

  private static final String TEST_ROOT = "br.com.f2e.ovenplatform";

  private static final DescribedPredicate<JavaClass> APPROVED_POSTGRES_CONTAINER_OWNER =
      hasName("br.com.f2e.ovenplatform.architecture.TestBuildArchitectureTests")
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.shared.infrastructure.persistence.test."
                      + "PostgresTestContainerConfiguration"))
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.identity.infrastructure.persistence."
                      + "TenantMembershipRolesMigrationTest"))
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.infrastructure.bootstrap."
                      + "OwnerProvisioningProcessIntegrationTest"));

  private static final DescribedPredicate<JavaClass> APPROVED_SPRING_BOOT_TEST =
      hasName("br.com.f2e.ovenplatform.e2e.CucumberSpringConfiguration")
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.infrastructure.bootstrap."
                      + "OwnerProvisioningApplicationIntegrationTest"))
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.kitchen.infrastructure.event."
                      + "KitchenModuleEventsIntegrationTest"))
          .or(hasName("br.com.f2e.ovenplatform.observability.ManagementEndpointsIntegrationTest"))
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.orders.infrastructure.event."
                      + "OrdersReadinessModuleEventsIntegrationTest"))
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.payment.infrastructure.event."
                      + "PaymentModuleEventsIntegrationTest"))
          .or(
              hasName(
                  "br.com.f2e.ovenplatform.shared.infrastructure.event."
                      + "EventPublicationRegistryIntegrationTest"));

  private static JavaClasses testClasses;

  @BeforeAll
  static void setUp() {
    testClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.OnlyIncludeTests())
            .importPackages(TEST_ROOT);
  }

  @Test
  void dataJpaTestsShouldUseTheSharedBaseClass() {
    classes()
        .that()
        .areAnnotatedWith(DataJpaTest.class)
        .should()
        .beAssignableTo(DataJpaIntegrationTest.class)
        .check(testClasses);
  }

  @Test
  void webMvcTestsShouldUseTheSharedBaseClass() {
    classes()
        .that()
        .areAnnotatedWith(WebMvcTest.class)
        .should()
        .beAssignableTo(AbstractControllerTest.class)
        .check(testClasses);
  }

  @Test
  void testsShouldNotDirtyTheSpringContext() {
    noClasses().should().beAnnotatedWith(DirtiesContext.class).check(testClasses);
    noMethods().should().beAnnotatedWith(DirtiesContext.class).check(testClasses);
  }

  @Test
  void postgresContainersShouldOnlyBeCreatedByApprovedTests() {
    noClasses()
        .that(not(APPROVED_POSTGRES_CONTAINER_OWNER))
        .should()
        .dependOnClassesThat()
        .areAssignableTo(PostgreSQLContainer.class)
        .check(testClasses);
  }

  @Test
  void fullSpringBootContextsShouldRemainExplicit() {
    classes()
        .that()
        .areAnnotatedWith(SpringBootTest.class)
        .should(satisfy(APPROVED_SPRING_BOOT_TEST, "be explicitly approved"))
        .check(testClasses);
  }

  private static DescribedPredicate<JavaClass> hasName(String className) {
    return new DescribedPredicate<>("have name " + className) {
      @Override
      public boolean test(JavaClass javaClass) {
        return javaClass.getName().equals(className);
      }
    };
  }

  private static ArchCondition<JavaClass> satisfy(
      DescribedPredicate<JavaClass> predicate, String description) {
    return new ArchCondition<>(description) {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        if (!predicate.test(javaClass)) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, javaClass.getName() + " is not explicitly approved"));
        }
      }
    };
  }
}
