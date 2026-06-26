package br.com.f2e.ovenplatform.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import br.com.f2e.ovenplatform.identity.domain.UserStatus;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaTenantMembershipRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaUserRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataTenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataUserRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.security.config.PasswordEncoderConfig;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  IdentityService.class,
  JpaUserRepositoryAdapter.class,
  JpaTenantMembershipRepositoryAdapter.class,
  PasswordEncoderConfig.class
})
@EnableJpaAuditing
class IdentityServiceIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ANOTHER_TENANT_ID =
      UUID.fromString("b7210129-f1d5-4942-8d0a-b144e518aecc");

  private static final String EMAIL = "contact@email.com";
  private static final String NORMALIZED_EMAIL = "contact@email.com";
  private static final String RAW_PASSWORD = "my-secret-password";

  @Autowired private IdentityService identityService;
  @Autowired private SpringDataUserRepository userRepository;
  @Autowired private SpringDataTenantMembershipRepository tenantMembershipRepository;
  @Autowired private EntityManager entityManager;

  @MockitoBean private TenantValidator tenantValidator;

  @Test
  void shouldCreateUserSuccessfully() {
    var user = createUserAndFlush(TENANT_ID, EMAIL, UserRole.ADMIN);

    assertUser(user, TENANT_ID, UserRole.ADMIN);
  }

  @Test
  void shouldHashPasswordBeforePersistingUser() {
    var user = createUserAndFlush(TENANT_ID, EMAIL, UserRole.ADMIN);

    assertNotEquals(RAW_PASSWORD, user.getPassword());
    assertThat(user.getPassword()).isNotNull();
    assertFalse(user.getPassword().isBlank());
    assertTrue(user.getPassword().startsWith("$2"));
  }

  @Test
  void shouldThrowExceptionForDuplicateEmailWithinSameTenant() {
    createUserAndFlush(TENANT_ID, EMAIL, UserRole.ADMIN);

    identityService.create(TENANT_ID, EMAIL, UUID.randomUUID().toString(), UserRole.OWNER);

    assertThrows(DataIntegrityViolationException.class, userRepository::flush);
  }

  @Test
  void shouldAllowSameEmailForDifferentTenants() {
    var user = createUserAndFlush(TENANT_ID, EMAIL, UserRole.ADMIN);
    var user2 = createUserAndFlush(ANOTHER_TENANT_ID, EMAIL, UserRole.OWNER);

    assertUser(user, TENANT_ID, UserRole.ADMIN);
    assertUser(user2, ANOTHER_TENANT_ID, UserRole.OWNER);
  }

  @Test
  void shouldNormalizeEmailBeforePersisting() {
    var user = createUserAndFlush(TENANT_ID, " Contact@email.com", UserRole.ADMIN);

    assertEquals(NORMALIZED_EMAIL, user.getEmail());
    assertEquals(TENANT_ID, user.getTenantId());
    assertEquals(UserRole.ADMIN, user.getRole());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
  }

  @Test
  void shouldThrowExceptionWhenEmailIsInvalid() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            identityService.create(
                TENANT_ID, "invalidEmailOutlook.com", RAW_PASSWORD, UserRole.MEMBER));
  }

  @Test
  void shouldFindTenantUserByIdThroughService() {
    var command =
        new CreateTenantUserCommand(TENANT_ID, EMAIL, RAW_PASSWORD, TenantMembershipRole.ADMIN);

    var created = identityService.createTenantUser(command);
    flushAndClear();

    var result = identityService.findTenantUserById(TENANT_ID, created.userId());

    assertThat(result.userId()).isEqualTo(created.userId());
    assertThat(result.tenantId()).isEqualTo(TENANT_ID);
    assertThat(result.email()).isEqualTo(NORMALIZED_EMAIL);
    assertThat(result.role()).isEqualTo(TenantMembershipRole.ADMIN);
    assertThat(result.status()).isEqualTo(TenantMembershipStatus.ACTIVE);

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldThrowWhenTenantUserDoesNotExistThroughService() {
    UUID userId = UUID.randomUUID();

    var exception =
        assertThrows(
            NoSuchElementException.class,
            () -> identityService.findTenantUserById(TENANT_ID, userId));

    assertEquals("User", exception.getMessage());
  }

  @Test
  void shouldThrowWhenUserDoesNotBelongToTenantThroughService() {
    var command =
        new CreateTenantUserCommand(TENANT_ID, EMAIL, RAW_PASSWORD, TenantMembershipRole.ADMIN);

    var created = identityService.createTenantUser(command);
    flushAndClear();

    var userId = created.userId();
    var exception =
        assertThrows(
            NoSuchElementException.class,
            () -> identityService.findTenantUserById(ANOTHER_TENANT_ID, userId));

    assertEquals("User", exception.getMessage());

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldCreateNewUserAndTenantMembershipWhenTenantExists() {
    var command =
        new CreateTenantUserCommand(TENANT_ID, EMAIL, RAW_PASSWORD, TenantMembershipRole.ADMIN);

    var response = identityService.createTenantUser(command);

    flushAndClear();

    var persistedUser = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow();
    var persistedMembership =
        tenantMembershipRepository.findAll().stream()
            .filter(membership -> membership.getUser().getId().equals(persistedUser.getId()))
            .findFirst()
            .orElseThrow();

    assertThat(response.userId()).isEqualTo(persistedUser.getId());
    assertThat(response.tenantId()).isEqualTo(TENANT_ID);
    assertThat(response.role()).isEqualTo(TenantMembershipRole.ADMIN);
    assertThat(response.status()).isEqualTo(TenantMembershipStatus.ACTIVE);

    assertThat(persistedMembership.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(persistedMembership.getUser().getId()).isEqualTo(persistedUser.getId());
    assertThat(persistedMembership.getRole()).isEqualTo(TenantMembershipRole.ADMIN);
    assertThat(persistedMembership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldReuseExistingUserAndCreateTenantMembershipWhenEmailAlreadyExists() {
    var existingUser = createUserAndFlush(TENANT_ID, EMAIL, UserRole.MEMBER);

    var command =
        new CreateTenantUserCommand(
            ANOTHER_TENANT_ID, EMAIL, RAW_PASSWORD, TenantMembershipRole.OWNER);

    var response = identityService.createTenantUser(command);

    flushAndClear();

    var users = userRepository.findAll();

    assertThat(users).hasSize(1);
    assertThat(response.userId()).isEqualTo(existingUser.getId());
    assertThat(response.tenantId()).isEqualTo(ANOTHER_TENANT_ID);
    assertThat(response.role()).isEqualTo(TenantMembershipRole.OWNER);
    assertThat(response.status()).isEqualTo(TenantMembershipStatus.ACTIVE);

    var memberships = tenantMembershipRepository.findAll();

    assertThat(memberships).hasSize(1);
    assertThat(memberships.getFirst().getUser().getId()).isEqualTo(existingUser.getId());
    assertThat(memberships.getFirst().getTenantId()).isEqualTo(ANOTHER_TENANT_ID);
    assertThat(memberships.getFirst().getRole()).isEqualTo(TenantMembershipRole.OWNER);

    verify(tenantValidator).ensureTenantExists(ANOTHER_TENANT_ID);
  }

  @Test
  void shouldNotCreateUserOrMembershipWhenTenantDoesNotExist() {
    doThrow(new ResourceNotFoundException("Tenant", TENANT_ID))
        .when(tenantValidator)
        .ensureTenantExists(TENANT_ID);

    var command =
        new CreateTenantUserCommand(TENANT_ID, EMAIL, RAW_PASSWORD, TenantMembershipRole.ADMIN);

    assertThatThrownBy(() -> identityService.createTenantUser(command))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThat(userRepository.findAll()).isEmpty();
    assertThat(tenantMembershipRepository.findAll()).isEmpty();

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldFailWhenUserAlreadyBelongsToTenant() {
    var command =
        new CreateTenantUserCommand(TENANT_ID, EMAIL, RAW_PASSWORD, TenantMembershipRole.ADMIN);

    identityService.createTenantUser(command);
    flushAndClear();

    assertThat(userRepository.findAll()).hasSize(1);
    assertThat(tenantMembershipRepository.findAll()).hasSize(1);

    assertThatThrownBy(
            () -> {
              identityService.createTenantUser(command);
              entityManager.flush();
            })
        .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);

    verify(tenantValidator, times(2)).ensureTenantExists(TENANT_ID);
  }

  @Test
  void shouldNormalizeEmailBeforeCreatingTenantUser() {
    var command =
        new CreateTenantUserCommand(
            TENANT_ID, " Contact@email.com ", RAW_PASSWORD, TenantMembershipRole.ADMIN);

    var response = identityService.createTenantUser(command);

    flushAndClear();

    var persistedUser = userRepository.findByEmail(NORMALIZED_EMAIL).orElseThrow();

    assertThat(response.userId()).isEqualTo(persistedUser.getId());
    assertThat(persistedUser.getEmail()).isEqualTo(NORMALIZED_EMAIL);

    verify(tenantValidator).ensureTenantExists(TENANT_ID);
  }

  private User createUserAndFlush(UUID tenantId, String email, UserRole role) {
    var user = identityService.create(tenantId, email, RAW_PASSWORD, role);
    userRepository.flush();
    return user;
  }

  private void flushAndClear() {
    userRepository.flush();
    tenantMembershipRepository.flush();
    entityManager.clear();
  }

  private static void assertUser(User user, UUID tenantId, UserRole expectedRole) {
    assertEquals(NORMALIZED_EMAIL, user.getEmail());
    assertEquals(tenantId, user.getTenantId());
    assertEquals(expectedRole, user.getRole());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
  }
}
