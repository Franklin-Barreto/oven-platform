package br.com.f2e.ovenplatform.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import br.com.f2e.ovenplatform.identity.domain.UserStatus;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.JpaUserRepositoryAdapter;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataUserRepository;
import br.com.f2e.ovenplatform.identity.infrastructure.security.config.PasswordEncoderConfig;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({IdentityService.class, JpaUserRepositoryAdapter.class, PasswordEncoderConfig.class})
@EnableJpaAuditing
class IdentityServiceIntegrationTest {

  private static final String EMAIL = "contact@email.com";
  private static final String RAW_PASSWORD = "my-secret-password";

  @Autowired private IdentityService identityService;
  @Autowired private SpringDataTenantRepository tenantRepository;
  @Autowired private SpringDataUserRepository userRepository;

  @Test
  void shouldCreateUserSuccessfully() {
    var tenant = createTenant();

    var user = createUserAndFlush(tenant.getId(), EMAIL, UserRole.ADMIN);

    assertUser(user, tenant, UserRole.ADMIN);
  }

  @Test
  void shouldHashPasswordBeforePersistingUser() {
    var tenant = createTenant();

    var user = createUserAndFlush(tenant.getId(), EMAIL, UserRole.ADMIN);

    assertNotEquals(RAW_PASSWORD, user.getPassword());
    assertFalse(user.getPassword().isBlank());
    assertTrue(user.getPassword().startsWith("$2"));
  }

  @Test
  void shouldThrowExceptionForDuplicateEmailWithinSameTenant() {
    var tenant = createTenant();

    createUserAndFlush(tenant.getId(), EMAIL, UserRole.ADMIN);

    identityService.create(tenant.getId(), EMAIL, UUID.randomUUID().toString(), UserRole.OWNER);

    assertThrows(DataIntegrityViolationException.class, userRepository::flush);
  }

  @Test
  void shouldAllowSameEmailForDifferentTenants() {
    var tenantA = createTenant();
    var tenantB = createTenant();

    var user = createUserAndFlush(tenantA.getId(), EMAIL, UserRole.ADMIN);
    var user2 = createUserAndFlush(tenantB.getId(), EMAIL, UserRole.OWNER);

    assertUser(user, tenantA, UserRole.ADMIN);
    assertUser(user2, tenantB, UserRole.OWNER);
  }

  @Test
  void shouldNormalizeEmailBeforePersisting() {
    var tenant = createTenant();

    var user = createUserAndFlush(tenant.getId(), " Contact@email.com", UserRole.ADMIN);

    assertEquals(EMAIL, user.getEmail());
    assertEquals(tenant.getId(), user.getTenantId());
    assertEquals(UserRole.ADMIN, user.getRole());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
  }

  @Test
  void shouldThrowExceptionWhenEmailIsInvalid() {
    var tenantId = createTenant().getId();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            identityService.create(
                tenantId, "invalidEmailOutlook.com", RAW_PASSWORD, UserRole.MEMBER));
  }

  @Test
  void shouldFindUserByIdAndTenantIdThroughService() {
    var tenant = createTenant();
    var user = createUserAndFlush(tenant.getId(), EMAIL, UserRole.ADMIN);

    var result = identityService.findByIdAndTenantId(user.getId(), tenant.getId());

    assertUser(result, tenant, UserRole.ADMIN);
  }

  @Test
  void shouldThrowWhenUserDoesNotExistThroughService() {
    var tenantId = createTenant().getId();
    UUID userId = UUID.randomUUID();

    var exception =
        assertThrows(
            NoSuchElementException.class,
            () -> identityService.findByIdAndTenantId(userId, tenantId));

    assertEquals("User", exception.getMessage());
  }

  @Test
  void shouldThrowWhenUserBelongsToAnotherTenantThroughService() {
    var tenantA = createTenant();
    var tenantBId = createTenant().getId();
    var userId = createUserAndFlush(tenantA.getId(), EMAIL, UserRole.ADMIN).getId();

    var exception =
        assertThrows(
            NoSuchElementException.class,
            () -> identityService.findByIdAndTenantId(userId, tenantBId));

    assertEquals("User", exception.getMessage());
  }

  private Tenant createTenant() {
    return tenantRepository.save(new Tenant("Don Corleone Pizzeria", Plan.MVP));
  }

  private User createUserAndFlush(UUID tenantId, String email, UserRole role) {
    var user =
        identityService.create(tenantId, email, IdentityServiceIntegrationTest.RAW_PASSWORD, role);
    userRepository.flush();
    return user;
  }

  private static void assertUser(User user, Tenant tenant, UserRole expectedRole) {
    assertEquals(EMAIL, user.getEmail());
    assertEquals(tenant.getId(), user.getTenantId());
    assertEquals(expectedRole, user.getRole());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
  }
}
