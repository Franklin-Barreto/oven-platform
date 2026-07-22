package br.com.f2e.ovenplatform.identity.application.team;

import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.ATTENDANT;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.KITCHEN;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole.MANAGER;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus.ACTIVE;
import static br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus.INACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.identity.application.port.TenantValidator;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaTenantMembershipRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaUserRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataTenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataUserRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.security.config.PasswordEncoderConfig;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({
  TenantTeamService.class,
  JpaUserRepositoryAdapter.class,
  JpaTenantMembershipRepositoryAdapter.class,
  PasswordEncoderConfig.class,
  TenantTeamManagementPolicy.class
})
class TenantTeamServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ANOTHER_TENANT_ID =
      UUID.fromString("b7210129-f1d5-4942-8d0a-b144e518aecc");

  private static final String EMAIL = "contact@email.com";
  private static final String NORMALIZED_EMAIL = "contact@email.com";
  private static final String RAW_PASSWORD = "my-secret-password";

  @Autowired private TenantTeamService tenantTeamService;
  @Autowired private SpringDataUserRepository userRepository;
  @Autowired private SpringDataTenantMembershipRepository tenantMembershipRepository;

  @MockitoBean private TenantValidator tenantValidator;

  @Test
  void shouldCreateNewUserAndTenantMembershipWhenTenantExists() {
    var actor = createOwner(TENANT_ID);
    var command =
        new CreateTenantUserCommand(TENANT_ID, actor.getId(), EMAIL, RAW_PASSWORD, Set.of(MANAGER));

    var response = tenantTeamService.createTenantUser(command);

    flushAndClear();

    var persistedUser = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow();
    var persistedMembership =
        tenantMembershipRepository.findAll().stream()
            .filter(membership -> membership.getUser().getId().equals(persistedUser.getId()))
            .findFirst()
            .orElseThrow();

    assertThat(response.userId()).isEqualTo(persistedUser.getId());
    assertThat(response.tenantId()).isEqualTo(TENANT_ID);
    assertThat(response.email()).isEqualTo(NORMALIZED_EMAIL);
    assertThat(response.roles()).contains(MANAGER);
    assertThat(response.status()).isEqualTo(ACTIVE);

    assertThat(persistedUser.getEmail()).isEqualTo(NORMALIZED_EMAIL);
    assertThat(persistedUser.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
    assertThat(persistedUser.getPasswordHash()).isNotBlank();
    assertThat(persistedUser.getPasswordHash()).startsWith("$2");

    assertThat(persistedMembership.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(persistedMembership.getUser().getId()).isEqualTo(persistedUser.getId());
    assertThat(persistedMembership.getRoles()).contains(MANAGER);
    assertThat(persistedMembership.getStatus()).isEqualTo(ACTIVE);

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldFindTenantUserByIdThroughService() {
    var actor = createOwner(TENANT_ID);
    var command =
        new CreateTenantUserCommand(TENANT_ID, actor.getId(), EMAIL, RAW_PASSWORD, Set.of(MANAGER));

    var created = tenantTeamService.createTenantUser(command);
    flushAndClear();

    var result = tenantTeamService.findTenantUserById(TENANT_ID, created.userId());

    assertThat(result.userId()).isEqualTo(created.userId());
    assertThat(result.tenantId()).isEqualTo(TENANT_ID);
    assertThat(result.email()).isEqualTo(NORMALIZED_EMAIL);
    assertThat(result.roles()).contains(MANAGER);
    assertThat(result.status()).isEqualTo(ACTIVE);

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldThrowWhenTenantUserDoesNotExistThroughService() {
    UUID userId = UUID.randomUUID();

    var exception =
        assertThrows(
            NoSuchElementException.class,
            () -> tenantTeamService.findTenantUserById(TENANT_ID, userId));

    assertEquals("User", exception.getMessage());
  }

  @Test
  void shouldThrowWhenUserDoesNotBelongToTenantThroughService() {
    var actor = createOwner(TENANT_ID);
    var command =
        new CreateTenantUserCommand(TENANT_ID, actor.getId(), EMAIL, RAW_PASSWORD, Set.of(MANAGER));

    var created = tenantTeamService.createTenantUser(command);
    flushAndClear();

    var userId = created.userId();

    var exception =
        assertThrows(
            NoSuchElementException.class,
            () -> tenantTeamService.findTenantUserById(ANOTHER_TENANT_ID, userId));

    assertEquals("User", exception.getMessage());

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldReuseExistingUserAndCreateTenantMembershipWhenEmailAlreadyExists() {
    var actor = createOwner(TENANT_ID);
    var firstTenantCommand =
        new CreateTenantUserCommand(
            TENANT_ID, actor.getId(), EMAIL, RAW_PASSWORD, Set.of(ATTENDANT));

    var existingUserResponse = tenantTeamService.createTenantUser(firstTenantCommand);
    flushAndClear();

    tenantMembershipRepository.save(TenantMembership.owner(actor, ANOTHER_TENANT_ID));
    var secondTenantCommand =
        new CreateTenantUserCommand(
            ANOTHER_TENANT_ID, actor.getId(), EMAIL, RAW_PASSWORD, Set.of(KITCHEN));

    var response = tenantTeamService.createTenantUser(secondTenantCommand);
    flushAndClear();

    var users = userRepository.findAll();

    assertThat(users).filteredOn(user -> EMAIL.equals(user.getEmail())).hasSize(1);
    assertThat(response.userId()).isEqualTo(existingUserResponse.userId());
    assertThat(response.tenantId()).isEqualTo(ANOTHER_TENANT_ID);
    assertThat(response.email()).isEqualTo(NORMALIZED_EMAIL);
    assertThat(response.roles()).containsExactly(KITCHEN);
    assertThat(response.status()).isEqualTo(ACTIVE);

    var memberships = tenantMembershipRepository.findAll();

    assertThat(memberships)
        .filteredOn(
            membership -> membership.getUser().getId().equals(existingUserResponse.userId()))
        .hasSize(2)
        .anySatisfy(
            membership -> {
              assertThat(membership.getUser().getId()).isEqualTo(existingUserResponse.userId());
              assertThat(membership.getTenantId()).isEqualTo(TENANT_ID);
              assertThat(membership.getRoles()).contains(ATTENDANT);
            })
        .anySatisfy(
            membership -> {
              assertThat(membership.getUser().getId()).isEqualTo(existingUserResponse.userId());
              assertThat(membership.getTenantId()).isEqualTo(ANOTHER_TENANT_ID);
              assertThat(membership.getRoles()).containsExactly(KITCHEN);
            });

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
    verify(tenantValidator).ensureTenantExists(ANOTHER_TENANT_ID);
  }

  @Test
  void shouldRejectOwnerInOrdinaryTenantUserCreation() {
    var actor = createOwner(TENANT_ID);
    var command =
        new CreateTenantUserCommand(
            TENANT_ID, actor.getId(), EMAIL, RAW_PASSWORD, Set.of(TenantMembershipRole.OWNER));

    assertThatThrownBy(() -> tenantTeamService.createTenantUser(command))
        .isInstanceOf(TenantTeamManagementDeniedException.class);
  }

  @Test
  void shouldNotCreateUserOrMembershipWhenTenantDoesNotExist() {
    doThrow(new ResourceNotFoundException("Tenant", TENANT_ID))
        .when(tenantValidator)
        .ensureTenantExists(TENANT_ID);

    var command =
        new CreateTenantUserCommand(
            TENANT_ID, UUID.randomUUID(), EMAIL, RAW_PASSWORD, Set.of(MANAGER));

    assertThatThrownBy(() -> tenantTeamService.createTenantUser(command))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThat(userRepository.findAll()).isEmpty();
    assertThat(tenantMembershipRepository.findAll()).isEmpty();

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldFailWhenUserAlreadyBelongsToTenant() {
    var actor = createOwner(TENANT_ID);
    var command =
        new CreateTenantUserCommand(TENANT_ID, actor.getId(), EMAIL, RAW_PASSWORD, Set.of(MANAGER));

    tenantTeamService.createTenantUser(command);
    flushAndClear();

    assertThat(userRepository.findAll()).hasSize(2);
    assertThat(tenantMembershipRepository.findAll()).hasSize(2);

    tenantTeamService.createTenantUser(command);

    assertThatThrownBy(() -> entityManager.flush())
        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);

    verify(tenantValidator, times(2)).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldNormalizeEmailBeforeCreatingTenantUser() {
    var actor = createOwner(TENANT_ID);
    var command =
        new CreateTenantUserCommand(
            TENANT_ID, actor.getId(), " Contact@email.com ", RAW_PASSWORD, Set.of(MANAGER));

    var response = tenantTeamService.createTenantUser(command);

    flushAndClear();

    var persistedUser = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow();

    assertThat(response.userId()).isEqualTo(persistedUser.getId());
    assertThat(response.email()).isEqualTo(NORMALIZED_EMAIL);
    assertThat(persistedUser.getEmail()).isEqualTo(NORMALIZED_EMAIL);

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldAllowOwnerToReplaceStaffMembershipRoles() {
    var owner = userRepository.save(new User("owner@test.com", "encoded-password"));
    var employee = userRepository.save(new User("employee@test.com", "encoded-password"));

    tenantMembershipRepository.save(TenantMembership.owner(owner, TENANT_ID));
    tenantMembershipRepository.save(TenantMembership.staff(employee, TENANT_ID, Set.of(ATTENDANT)));

    var command =
        new ReplaceTenantMembershipRolesCommand(
            TENANT_ID, owner.getId(), employee.getId(), Set.of(ATTENDANT, KITCHEN));

    var result = tenantTeamService.replaceTenantMembershipRoles(command);

    flushAndClear();

    var persistedMembership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(employee.getId(), TENANT_ID)
            .orElseThrow();

    assertThat(result.roles()).containsExactlyInAnyOrder(ATTENDANT, KITCHEN);
    assertThat(persistedMembership.getRoles()).containsExactlyInAnyOrder(ATTENDANT, KITCHEN);
  }

  @Test
  void shouldAllowManagerToReplaceStaffMembershipRoles() {
    var manager = userRepository.save(new User("manager@test.com", "encoded-password"));
    var employee = userRepository.save(new User("employee@test.com", "encoded-password"));

    tenantMembershipRepository.save(TenantMembership.staff(manager, TENANT_ID, Set.of(MANAGER)));
    tenantMembershipRepository.save(TenantMembership.staff(employee, TENANT_ID, Set.of(ATTENDANT)));

    var command =
        new ReplaceTenantMembershipRolesCommand(
            TENANT_ID, manager.getId(), employee.getId(), Set.of(ATTENDANT, KITCHEN));

    tenantTeamService.replaceTenantMembershipRoles(command);

    flushAndClear();

    var persistedMembership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(employee.getId(), TENANT_ID)
            .orElseThrow();

    assertThat(persistedMembership.getRoles()).containsExactlyInAnyOrder(ATTENDANT, KITCHEN);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("forbiddenManagerRoleChanges")
  void shouldRejectForbiddenManagerRoleChanges(
      String scenario,
      Set<TenantMembershipRole> currentTargetRoles,
      Set<TenantMembershipRole> requestedRoles) {

    var manager = userRepository.save(new User("manager@test.com", "encoded-password"));
    var target = userRepository.save(new User("target@test.com", "encoded-password"));

    tenantMembershipRepository.save(TenantMembership.staff(manager, TENANT_ID, Set.of(MANAGER)));
    tenantMembershipRepository.save(TenantMembership.staff(target, TENANT_ID, currentTargetRoles));

    var command =
        new ReplaceTenantMembershipRolesCommand(
            TENANT_ID, manager.getId(), target.getId(), requestedRoles);

    assertThatThrownBy(() -> tenantTeamService.replaceTenantMembershipRoles(command))
        .isInstanceOf(TenantTeamManagementDeniedException.class);

    flushAndClear();

    var persistedMembership =
        tenantMembershipRepository.findByUserIdAndTenantId(target.getId(), TENANT_ID).orElseThrow();

    assertThat(persistedMembership.getRoles())
        .containsExactlyInAnyOrderElementsOf(currentTargetRoles);
  }

  @Test
  void shouldReturnNotFoundWhenTargetMembershipBelongsToAnotherTenant() {
    var owner = userRepository.save(new User("owner@test.com", "encoded-password"));
    var target = userRepository.save(new User("target@test.com", "encoded-password"));

    tenantMembershipRepository.save(TenantMembership.owner(owner, TENANT_ID));
    tenantMembershipRepository.save(
        TenantMembership.staff(target, ANOTHER_TENANT_ID, Set.of(ATTENDANT)));

    var command =
        new ReplaceTenantMembershipRolesCommand(
            TENANT_ID, owner.getId(), target.getId(), Set.of(ATTENDANT, KITCHEN));

    assertThatThrownBy(() -> tenantTeamService.replaceTenantMembershipRoles(command))
        .isInstanceOf(ResourceNotFoundException.class);

    flushAndClear();

    var otherTenantMembership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(target.getId(), ANOTHER_TENANT_ID)
            .orElseThrow();

    assertThat(otherTenantMembership.getRoles()).containsExactly(ATTENDANT);
  }

  @Test
  void shouldReturnNotFoundWhenActorDoesNotBelongToTenant() {
    var actor = userRepository.save(new User("manager@test.com", "encoded-password"));
    var target = userRepository.save(new User("target@test.com", "encoded-password"));

    tenantMembershipRepository.save(
        TenantMembership.staff(actor, ANOTHER_TENANT_ID, Set.of(MANAGER)));
    tenantMembershipRepository.save(TenantMembership.staff(target, TENANT_ID, Set.of(ATTENDANT)));

    var command =
        new ReplaceTenantMembershipRolesCommand(
            TENANT_ID, actor.getId(), target.getId(), Set.of(ATTENDANT, KITCHEN));

    assertThatThrownBy(() -> tenantTeamService.replaceTenantMembershipRoles(command))
        .isInstanceOf(ResourceNotFoundException.class);

    flushAndClear();

    var targetMembership =
        tenantMembershipRepository.findByUserIdAndTenantId(target.getId(), TENANT_ID).orElseThrow();

    assertThat(targetMembership.getRoles()).containsExactly(ATTENDANT);
  }

  @Test
  void shouldListOnlyMembershipsFromRequestedTenant() {
    var owner = userRepository.save(new User("owner@test.com", "encoded-password"));
    var employee = userRepository.save(new User("employee@test.com", "encoded-password"));
    var anotherTenantUser = userRepository.save(new User("other@test.com", "encoded-password"));

    tenantMembershipRepository.save(TenantMembership.owner(owner, TENANT_ID));
    tenantMembershipRepository.save(
        TenantMembership.staff(employee, TENANT_ID, Set.of(ATTENDANT, KITCHEN)));
    tenantMembershipRepository.save(
        TenantMembership.staff(anotherTenantUser, ANOTHER_TENANT_ID, Set.of(MANAGER)));

    flushAndClear();

    var result = tenantTeamService.listTenantMemberships(TENANT_ID);

    assertThat(result)
        .hasSize(2)
        .extracting(TenantUserResult::userId)
        .containsExactlyInAnyOrder(owner.getId(), employee.getId());
  }

  @Test
  void shouldAllowOwnerToDeactivateStaffMembershipIdempotently() {
    var owner = userRepository.save(new User("owner@test.com", "encoded-password"));
    var employee = userRepository.save(new User("employee@test.com", "encoded-password"));

    tenantMembershipRepository.save(TenantMembership.owner(owner, TENANT_ID));
    tenantMembershipRepository.save(TenantMembership.staff(employee, TENANT_ID, Set.of(ATTENDANT)));

    var command = new DeactivateTenantMembershipCommand(TENANT_ID, owner.getId(), employee.getId());

    tenantTeamService.deactivateTenantMembership(command);
    tenantTeamService.deactivateTenantMembership(command);

    flushAndClear();

    var persistedMembership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(employee.getId(), TENANT_ID)
            .orElseThrow();

    assertThat(persistedMembership.getStatus()).isEqualTo(INACTIVE);
  }

  @Test
  void shouldAllowOwnerToReactivateStaffMembershipIdempotently() {
    var owner = userRepository.save(new User("owner@test.com", "encoded-password"));
    var employee = userRepository.save(new User("employee@test.com", "encoded-password"));

    tenantMembershipRepository.save(TenantMembership.owner(owner, TENANT_ID));

    var employeeMembership = TenantMembership.staff(employee, TENANT_ID, Set.of(ATTENDANT));
    employeeMembership.deactivate();
    tenantMembershipRepository.save(employeeMembership);

    var command = new ReactivateTenantMembershipCommand(TENANT_ID, owner.getId(), employee.getId());

    tenantTeamService.reactivateTenantMembership(command);
    tenantTeamService.reactivateTenantMembership(command);

    flushAndClear();

    var persistedMembership =
        tenantMembershipRepository
            .findByUserIdAndTenantId(employee.getId(), TENANT_ID)
            .orElseThrow();

    assertThat(persistedMembership.getStatus()).isEqualTo(ACTIVE);
  }

  private static Stream<Arguments> forbiddenManagerRoleChanges() {
    return Stream.of(
        Arguments.of("manager cannot assign manager", Set.of(ATTENDANT), Set.of(MANAGER)),
        Arguments.of("manager cannot remove manager", Set.of(MANAGER), Set.of(ATTENDANT)));
  }

  private User createOwner(UUID tenantId) {
    var owner =
        userRepository.save(new User("owner-" + UUID.randomUUID() + "@test.com", "password-hash"));
    tenantMembershipRepository.save(TenantMembership.owner(owner, tenantId));
    return owner;
  }
}
