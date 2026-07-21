package br.com.f2e.ovenplatform.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TenantMembershipTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final String USER_EMAIL = "user.email@outlook.com";
  private static final String RAW_PASSWORD = "my-secret-password";

  @Test
  void shouldCreateTenantMembership() {
    var user = createUser();

    var tenantMembership = TenantMembership.owner(user, TENANT_ID);

    assertThat(tenantMembership.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(tenantMembership.getUser()).isEqualTo(user);
    assertThat(tenantMembership.getUser().getEmail()).isEqualTo(USER_EMAIL);
    assertThat(tenantMembership.getRoles()).containsExactly(TenantMembershipRole.OWNER);
    assertThat(tenantMembership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
  }

  @Test
  void shouldRejectNullRoleElement() {
    var roles = new HashSet<TenantMembershipRole>();
    roles.add(null);
    var user = createUser();

    assertThatThrownBy(() -> TenantMembership.staff(user, TENANT_ID, roles))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roles must not contain null elements");
  }

  @Test
  void shouldCreateStaffMembershipWithMultipleRoles() {
    var membership =
        TenantMembership.staff(
            createUser(),
            TENANT_ID,
            Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN));

    assertThat(membership.getRoles())
        .containsExactlyInAnyOrder(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN);
  }

  @Test
  void shouldRejectNullRoles() {
    var user = createUser();

    assertThatThrownBy(() -> TenantMembership.staff(user, TENANT_ID, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roles must not be null");
  }

  @Test
  void shouldRejectEmptyRoles() {
    var user = createUser();
    var roles = Set.<TenantMembershipRole>of();

    assertThatThrownBy(() -> TenantMembership.staff(user, TENANT_ID, roles))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roles must have at least 1 item");
  }

  @Test
  void shouldRejectOwnerCombinedWithAnotherRole() {
    var user = createUser();
    var roles = Set.of(TenantMembershipRole.OWNER, TenantMembershipRole.ATTENDANT);

    assertThatThrownBy(() -> TenantMembership.staff(user, TENANT_ID, roles))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Staff membership cannot contain OWNER");
  }

  @Test
  void shouldDefensivelyCopyRoles() {
    var roles = new HashSet<>(Set.of(TenantMembershipRole.ATTENDANT));
    var membership = TenantMembership.staff(createUser(), TENANT_ID, roles);

    roles.add(TenantMembershipRole.KITCHEN);

    assertThat(membership.getRoles()).containsExactly(TenantMembershipRole.ATTENDANT);
    var membershipRoles = membership.getRoles();
    assertThatThrownBy(() -> membershipRoles.add(TenantMembershipRole.KITCHEN))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedTest
  @MethodSource("invalidData")
  void shouldRejectTenantMembershipWithInvalidData(
      User user, UUID tenantId, TenantMembershipRole role, String message) {
    assertThatThrownBy(() -> createTenantMembership(user, tenantId, role))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(message);
  }

  private static User createUser() {
    return new User(USER_EMAIL, RAW_PASSWORD);
  }

  private static Stream<Arguments> invalidData() {
    return Stream.of(
        Arguments.of(null, TENANT_ID, TenantMembershipRole.OWNER, "user must not be null"),
        Arguments.of(createUser(), null, TenantMembershipRole.OWNER, "tenantId must not be null"));
  }

  private void createTenantMembership(User user, UUID tenantId, TenantMembershipRole role) {
    if (TenantMembershipRole.OWNER.equals(role)) {
      TenantMembership.owner(user, tenantId);
      return;
    }
    var roles = new HashSet<TenantMembershipRole>();
    roles.add(role);
    TenantMembership.staff(user, tenantId, roles);
  }
}
