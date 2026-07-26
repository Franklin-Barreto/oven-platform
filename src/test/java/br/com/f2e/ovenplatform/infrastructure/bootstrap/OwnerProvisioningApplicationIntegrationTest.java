package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataTenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataUserRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("bootstrap-owner")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "oven.bootstrap.owner.tenant-name=Don Corleone Pizzeria",
      "oven.bootstrap.owner.email=owner@oven.local",
      "oven.bootstrap.owner.password=OwnerPass123!",
      "jwt.expiration-minutes=30"
    })
class OwnerProvisioningApplicationIntegrationTest {

  @Autowired private SpringDataTenantRepository tenantRepository;
  @Autowired private SpringDataUserRepository userRepository;
  @Autowired private SpringDataTenantMembershipRepository membershipRepository;

  @DynamicPropertySource
  static void jwtProperties(DynamicPropertyRegistry registry) {
    registry.add("jwt.secret", OwnerProvisioningApplicationIntegrationTest::testJwtSecret);
  }

  @Test
  void shouldStartNonWebContextAndProvisionOwner() {
    var tenant = tenantRepository.findByName("Don Corleone Pizzeria").orElseThrow();
    var user = userRepository.findByEmail("owner@oven.local").orElseThrow();
    var membership =
        membershipRepository.findByUserIdAndTenantId(user.getId(), tenant.getId()).orElseThrow();

    assertThat(membership.getRoles()).contains(TenantMembershipRole.OWNER);
    assertThat(membership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
  }

  private static String testJwtSecret() {
    var clearlyNonSecretTestValue = "test-jwt-signing-key-".repeat(4);
    return Base64.getEncoder().encodeToString(clearlyNonSecretTestValue.getBytes(UTF_8));
  }
}
