package br.com.f2e.ovenplatform.identity.domain;

import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.ATTENDANT;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.KITCHEN;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.OWNER;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus.ACTIVE;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus.INACTIVE;
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
    assertThat(tenantMembership.getRoles()).containsExactly(OWNER);
    assertThat(tenantMembership.getStatus()).isEqualTo(ACTIVE);
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
    var membership = TenantMembership.staff(createUser(), TENANT_ID, Set.of(ATTENDANT, KITCHEN));

    assertThat(membership.getRoles()).containsExactlyInAnyOrder(ATTENDANT, KITCHEN);
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
    var roles = Set.of(OWNER, ATTENDANT);

    assertThatThrownBy(() -> TenantMembership.staff(user, TENANT_ID, roles))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Staff membership cannot contain OWNER");
  }

  @Test
  void shouldDefensivelyCopyRoles() {
    var roles = new HashSet<>(Set.of(ATTENDANT));
    var membership = TenantMembership.staff(createUser(), TENANT_ID, roles);

    roles.add(KITCHEN);

    assertThat(membership.getRoles()).containsExactly(ATTENDANT);
    var membershipRoles = membership.getRoles();
    assertThatThrownBy(() -> membershipRoles.add(KITCHEN))
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

  @Test
  void shouldReplaceOperationalRoles() {
    var membership = staffMembership(ATTENDANT);

    membership.changeOperationalRolesTo(Set.of(ATTENDANT, KITCHEN));

    assertThat(membership.getRoles()).containsExactlyInAnyOrder(ATTENDANT, KITCHEN);
  }

  @Test
  void shouldRejectOwnerWhenChangingOperationalRoles() {
    var membership = staffMembership(ATTENDANT);
    var owner = Set.of(OWNER);

    assertThatThrownBy(() -> membership.changeOperationalRolesTo(owner))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(membership.getRoles()).containsExactly(ATTENDANT);
  }

  @Test
  void shouldDefensivelyCopyChangedOperationalRoles() {
    var membership = staffMembership(ATTENDANT);
    var newRoles = new HashSet<>(Set.of(ATTENDANT));

    membership.changeOperationalRolesTo(newRoles);
    newRoles.add(KITCHEN);

    assertThat(membership.getRoles()).containsExactly(ATTENDANT);
  }

  @Test
  void shouldDeactivateMembershipIdempotently() {
    var membership = staffMembership(ATTENDANT);

    membership.deactivate();
    membership.deactivate();

    assertThat(membership.getStatus()).isEqualTo(INACTIVE);
  }

  @Test
  void shouldReactivateMembershipIdempotently() {
    var membership = staffMembership(ATTENDANT);
    membership.deactivate();

    membership.activate();
    membership.activate();

    assertThat(membership.getStatus()).isEqualTo(ACTIVE);
  }

  private static TenantMembership staffMembership(TenantMembershipRole... roles) {
    return TenantMembership.staff(
        new User("employee@oven.test", "encoded-password"), TENANT_ID, Set.of(roles));
  }

  private static User createUser() {
    return new User(USER_EMAIL, RAW_PASSWORD);
  }

  private static Stream<Arguments> invalidData() {
    return Stream.of(
        Arguments.of(null, TENANT_ID, OWNER, "user must not be null"),
        Arguments.of(createUser(), null, OWNER, "tenantId must not be null"));
  }

  private void createTenantMembership(User user, UUID tenantId, TenantMembershipRole role) {
    if (OWNER.equals(role)) {
      TenantMembership.owner(user, tenantId);
      return;
    }
    var roles = new HashSet<TenantMembershipRole>();
    roles.add(role);
    TenantMembership.staff(user, tenantId, roles);
  }
}
