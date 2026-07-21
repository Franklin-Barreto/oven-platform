package br.com.f2e.ovenplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.application.security.TenantPermissionResolver;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaTenantMembershipRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataTenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataUserRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  TenantMembershipAuthenticationService.class,
  TenantPermissionResolver.class,
  JpaTenantMembershipRepositoryAdapter.class
})
class TenantMembershipAuthenticationServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");

  @Autowired private TenantMembershipAuthenticationService authenticationService;
  @Autowired private SpringDataUserRepository userRepository;
  @Autowired private SpringDataTenantMembershipRepository membershipRepository;

  @Test
  void shouldResolvePermissionsFromPersistedMembershipRoles() {
    var user = userRepository.save(new User("user@email.com", "password-hash"));
    membershipRepository.save(
        TenantMembership.staff(
            user, TENANT_ID, Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN)));
    flushAndClear();

    var authenticatedMembership =
        authenticationService.loadActiveMembership(user.getId(), TENANT_ID);

    assertThat(authenticatedMembership.userId()).isEqualTo(user.getId());
    assertThat(authenticatedMembership.tenantId()).isEqualTo(TENANT_ID);
    assertThat(authenticatedMembership.roles())
        .containsExactlyInAnyOrder(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN);
    assertThat(authenticatedMembership.permissions())
        .containsExactlyInAnyOrder(
            TenantPermission.CATALOG_READ,
            TenantPermission.CUSTOMER_READ,
            TenantPermission.CUSTOMER_MANAGE,
            TenantPermission.ORDER_READ,
            TenantPermission.ORDER_CREATE,
            TenantPermission.ORDER_MANAGE,
            TenantPermission.PAYMENT_READ,
            TenantPermission.PAYMENT_MANAGE,
            TenantPermission.KITCHEN_READ,
            TenantPermission.KITCHEN_OPERATE);
  }
}
