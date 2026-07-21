package br.com.f2e.ovenplatform.identity.application;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.application.security.TenantPermissionResolver;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantMembershipAuthenticationServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();

  @Mock private TenantMembershipRepository tenantMembershipRepository;
  @Mock private TenantPermissionResolver tenantPermissionResolver;

  @InjectMocks private TenantMembershipAuthenticationService service;

  @Test
  void shouldLoadCurrentActiveMembership() {
    var user = withId(new User("user@email.com", "password-hash"), USER_ID);
    var membership =
        TenantMembership.staff(
            user, TENANT_ID, Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN));
    var roles = Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN);

    when(tenantPermissionResolver.resolve(roles)).thenReturn(PERMISSIONS);
    when(tenantMembershipRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
        .thenReturn(Optional.of(membership));

    var authenticatedMembership = service.loadActiveMembership(USER_ID, TENANT_ID);

    assertThat(authenticatedMembership.userId()).isEqualTo(USER_ID);
    assertThat(authenticatedMembership.tenantId()).isEqualTo(TENANT_ID);
    assertThat(authenticatedMembership.roles())
        .containsExactlyInAnyOrder(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN);
    assertThat(authenticatedMembership.permissions())
        .containsExactlyInAnyOrderElementsOf(PERMISSIONS);

    verify(tenantPermissionResolver).resolve(roles);
  }

  @Test
  void shouldRejectAbsentMembership() {
    when(tenantMembershipRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadActiveMembership(USER_ID, TENANT_ID))
        .isInstanceOf(TenantAccessDeniedException.class);

    verifyNoInteractions(tenantPermissionResolver);
  }

  @Test
  void shouldRejectInactiveMembership() {
    var user = withId(new User("user@email.com", "password-hash"), USER_ID);
    var membership = TenantMembership.staff(user, TENANT_ID, Set.of(TenantMembershipRole.MANAGER));
    membership.deactivate();
    when(tenantMembershipRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
        .thenReturn(Optional.of(membership));

    assertThatThrownBy(() -> service.loadActiveMembership(USER_ID, TENANT_ID))
        .isInstanceOf(TenantMembershipInactiveException.class);
    verifyNoInteractions(tenantPermissionResolver);
  }

  private static final Set<TenantPermission> PERMISSIONS =
      Set.of(
          TenantPermission.CATALOG_READ,
          TenantPermission.KITCHEN_READ,
          TenantPermission.KITCHEN_OPERATE);
}
