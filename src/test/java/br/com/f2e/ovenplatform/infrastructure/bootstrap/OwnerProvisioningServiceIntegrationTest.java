package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaTenantMembershipRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaUserRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataTenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataUserRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.security.config.PasswordEncoderConfig;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.JpaTenantRepositoryAdapter;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Import({
  OwnerProvisioningService.class,
  JpaTenantRepositoryAdapter.class,
  JpaUserRepositoryAdapter.class,
  JpaTenantMembershipRepositoryAdapter.class,
  PasswordEncoderConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OwnerProvisioningServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final String TENANT_NAME = "Don Corleone Pizzeria";
  private static final String OWNER_EMAIL = "OWNER@OVEN.LOCAL";
  private static final String NORMALIZED_OWNER_EMAIL = "owner@oven.local";
  private static final String OWNER_PASSWORD = "OwnerPass123!";

  @Autowired private OwnerProvisioningService provisioningService;
  @Autowired private SpringDataTenantRepository tenantRepository;
  @Autowired private SpringDataUserRepository userRepository;
  @Autowired private SpringDataTenantMembershipRepository membershipRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void cleanDatabase() {
    membershipRepository.deleteAll();
    userRepository.deleteAll();
    tenantRepository.deleteAll();
  }

  @Test
  void shouldProvisionTenantUserAndActiveOwnerMembership() {
    var result = provisioningService.provision(command());

    var tenant = tenantRepository.findById(result.tenantId()).orElseThrow();
    var user = userRepository.findById(result.userId()).orElseThrow();
    var membership =
        membershipRepository.findByUserIdAndTenantId(user.getId(), tenant.getId()).orElseThrow();

    assertThat(result.outcome()).isEqualTo(OwnerProvisioningResult.Outcome.PROVISIONED);
    assertThat(tenant.getName()).isEqualTo(TENANT_NAME);
    assertThat(tenant.getPlan()).isEqualTo(Plan.MVP);
    assertThat(user.getEmail()).isEqualTo(NORMALIZED_OWNER_EMAIL);
    assertThat(passwordEncoder.matches(OWNER_PASSWORD, user.getPasswordHash())).isTrue();
    assertThat(membership.getRoles()).contains(TenantMembershipRole.OWNER);
    assertThat(membership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
  }

  @Test
  void shouldBeIdempotentWhenOwnerIsAlreadyProvisioned() {
    var firstResult = provisioningService.provision(command());

    var secondResult = provisioningService.provision(command());

    assertThat(secondResult.tenantId()).isEqualTo(firstResult.tenantId());
    assertThat(secondResult.userId()).isEqualTo(firstResult.userId());
    assertThat(secondResult.outcome())
        .isEqualTo(OwnerProvisioningResult.Outcome.ALREADY_PROVISIONED);
    assertThat(tenantRepository.count()).isOne();
    assertThat(userRepository.count()).isOne();
    assertThat(membershipRepository.count()).isOne();
  }

  @Test
  void shouldRollBackNewTenantWhenExistingUserPasswordDoesNotMatch() {
    userRepository.save(
        new User(NORMALIZED_OWNER_EMAIL, passwordEncoder.encode("ExistingPass123!")));

    var conflictingCommand = new OwnerProvisioningCommand(TENANT_NAME, OWNER_EMAIL, OWNER_PASSWORD);

    assertThatThrownBy(() -> provisioningService.provision(conflictingCommand))
        .isInstanceOf(OwnerProvisioningConflictException.class)
        .hasMessage("Existing user credentials do not match the bootstrap secret");

    assertThat(tenantRepository.count()).isZero();
    assertThat(userRepository.count()).isOne();
    assertThat(membershipRepository.count()).isZero();
  }

  @Test
  void shouldRejectExistingMembershipWithDifferentRole() {
    var tenant = tenantRepository.save(new Tenant(TENANT_NAME, Plan.MVP));
    var user =
        userRepository.save(
            new User(NORMALIZED_OWNER_EMAIL, passwordEncoder.encode(OWNER_PASSWORD)));
    membershipRepository.save(
        TenantMembership.staff(user, tenant.getId(), Set.of(TenantMembershipRole.MANAGER)));

    var command = command();

    assertThatThrownBy(() -> provisioningService.provision(command))
        .isInstanceOf(OwnerProvisioningConflictException.class)
        .hasMessage("Existing membership does not have OWNER role");
  }

  @Test
  void shouldRejectInactiveOwnerMembership() {
    var tenant = tenantRepository.save(new Tenant(TENANT_NAME, Plan.MVP));
    var user =
        userRepository.save(
            new User(NORMALIZED_OWNER_EMAIL, passwordEncoder.encode(OWNER_PASSWORD)));
    var membership = TenantMembership.owner(user, tenant.getId());
    membership.deactivate();
    membershipRepository.save(membership);

    var command = command();

    assertThatThrownBy(() -> provisioningService.provision(command))
        .isInstanceOf(OwnerProvisioningConflictException.class)
        .hasMessage("Existing OWNER membership is not active");
  }

  private static OwnerProvisioningCommand command() {
    return new OwnerProvisioningCommand(TENANT_NAME, OWNER_EMAIL, OWNER_PASSWORD);
  }
}
