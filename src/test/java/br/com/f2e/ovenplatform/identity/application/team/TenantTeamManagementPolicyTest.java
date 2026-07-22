package br.com.f2e.ovenplatform.identity.application.team;

import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.ATTENDANT;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.KITCHEN;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.MANAGER;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.OWNER;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TenantTeamManagementPolicyTest {

  private final TenantTeamManagementPolicy policy = new TenantTeamManagementPolicy();

  @Test
  void shouldAllowOwnerToChangeManagerOperationalRoles() {
    assertThatCode(
            () ->
                policy.ensureCanChangeRoles(
                    Set.of(OWNER), Set.of(MANAGER), Set.of(ATTENDANT, KITCHEN)))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowManagerToChangeStaffOperationalRoles() {
    assertThatCode(
            () ->
                policy.ensureCanChangeRoles(
                    Set.of(MANAGER), Set.of(ATTENDANT), Set.of(ATTENDANT, KITCHEN)))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("deniedRoleChanges")
  void shouldRejectUnauthorizedRoleChanges(
      String scenario,
      Set<TenantMembershipRole> actorRoles,
      Set<TenantMembershipRole> currentTargetRoles,
      Set<TenantMembershipRole> requestedRoles) {

    assertThatThrownBy(
            () -> policy.ensureCanChangeRoles(actorRoles, currentTargetRoles, requestedRoles))
        .isInstanceOf(TenantTeamManagementDeniedException.class);
  }

  @Test
  void shouldAllowOwnerToManageManagerMembership() {
    assertThatCode(() -> policy.ensureCanManageMembership(Set.of(OWNER), Set.of(MANAGER)))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowManagerToManageStaffMembership() {
    assertThatCode(
            () -> policy.ensureCanManageMembership(Set.of(MANAGER), Set.of(ATTENDANT, KITCHEN)))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("deniedMembershipManagement")
  void shouldRejectUnauthorizedMembershipManagement(
      Set<TenantMembershipRole> actorRoles, Set<TenantMembershipRole> targetRoles) {

    assertThatThrownBy(() -> policy.ensureCanManageMembership(actorRoles, targetRoles))
        .isInstanceOf(TenantTeamManagementDeniedException.class);
  }

  private static Stream<Arguments> deniedRoleChanges() {
    return Stream.of(
        Arguments.of(
            "manager assigns manager", Set.of(MANAGER), Set.of(ATTENDANT), Set.of(MANAGER)),
        Arguments.of(
            "manager removes manager", Set.of(MANAGER), Set.of(MANAGER), Set.of(ATTENDANT)),
        Arguments.of("owner assigns owner", Set.of(OWNER), Set.of(ATTENDANT), Set.of(OWNER)),
        Arguments.of(
            "owner changes owner membership", Set.of(OWNER), Set.of(OWNER), Set.of(ATTENDANT)),
        Arguments.of(
            "forbidden owner is hidden among operational roles",
            Set.of(OWNER),
            Set.of(ATTENDANT),
            Set.of(ATTENDANT, OWNER)),
        Arguments.of(
            "ordinary staff tries to manage membership",
            Set.of(ATTENDANT),
            Set.of(KITCHEN),
            Set.of(ATTENDANT)));
  }

  private static Stream<Arguments> deniedMembershipManagement() {
    return Stream.of(
        Arguments.of(Set.of(OWNER), Set.of(OWNER)),
        Arguments.of(Set.of(MANAGER), Set.of(MANAGER)),
        Arguments.of(Set.of(ATTENDANT), Set.of(KITCHEN)));
  }
}
