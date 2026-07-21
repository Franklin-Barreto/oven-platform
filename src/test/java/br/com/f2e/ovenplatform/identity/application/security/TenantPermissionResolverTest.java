package br.com.f2e.ovenplatform.identity.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TenantPermissionResolverTest {

  private final TenantPermissionResolver resolver = new TenantPermissionResolver();

  @Test
  void shouldResolveKitchenPermissions() {
    var permissions = resolver.resolve(Set.of(TenantMembershipRole.KITCHEN));

    assertThat(permissions)
        .containsExactlyInAnyOrder(TenantPermission.KITCHEN_READ, TenantPermission.KITCHEN_OPERATE);
  }

  @Test
  void shouldResolveAttendantPermissions() {
    var permissions = resolver.resolve(Set.of(TenantMembershipRole.ATTENDANT));

    assertThat(permissions)
        .containsExactlyInAnyOrder(
            TenantPermission.CATALOG_READ,
            TenantPermission.CUSTOMER_READ,
            TenantPermission.CUSTOMER_MANAGE,
            TenantPermission.ORDER_READ,
            TenantPermission.ORDER_CREATE,
            TenantPermission.ORDER_MANAGE,
            TenantPermission.PAYMENT_READ,
            TenantPermission.PAYMENT_MANAGE);
  }

  @Test
  void shouldResolveManagerPermissions() {
    var permissions = resolver.resolve(Set.of(TenantMembershipRole.MANAGER));

    assertThat(permissions).containsExactlyInAnyOrder(TenantPermission.values());
  }

  @Test
  void shouldResolveEveryPermissionForOwner() {
    var permissions = resolver.resolve(Set.of(TenantMembershipRole.OWNER));

    assertThat(permissions).containsExactlyInAnyOrder(TenantPermission.values());
  }

  @Test
  void shouldResolveUnionOfPermissionsForMultipleRoles() {
    var permissions =
        resolver.resolve(Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN));

    assertThat(permissions)
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

  @Test
  void shouldCollapseDuplicatePermissionsFromMultipleRoles() {
    var permissions =
        resolver.resolve(Set.of(TenantMembershipRole.MANAGER, TenantMembershipRole.KITCHEN));

    assertThat(permissions)
        .containsExactlyInAnyOrder(TenantPermission.values())
        .doesNotHaveDuplicates();
  }

  @Test
  void shouldReturnImmutablePermissions() {
    var permissions = resolver.resolve(Set.of(TenantMembershipRole.KITCHEN));

    assertThatThrownBy(() -> permissions.add(TenantPermission.ORDER_READ))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldRejectNullRoles() {
    assertThatThrownBy(() -> resolver.resolve(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roles must not be null");
  }

  @Test
  void shouldRejectEmptyRoles() {
    var roles = Set.<TenantMembershipRole>of();

    assertThatThrownBy(() -> resolver.resolve(roles))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roles must have at least 1 item");
  }

  @Test
  void shouldRejectNullRoleElement() {
    var roles = new HashSet<TenantMembershipRole>();
    roles.add(null);

    assertThatThrownBy(() -> resolver.resolve(roles))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roles must not contain null elements");
  }
}
