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
import br.com.f2e.ovenplatform.identity.infrastructure.security.BCryptPasswordHasher;
import br.com.f2e.ovenplatform.identity.infrastructure.security.config.SecurityConfig;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
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
@Import({
  IdentityService.class,
  BCryptPasswordHasher.class,
  JpaUserRepositoryAdapter.class,
  SecurityConfig.class
})
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

    var user = createUserAndFlush(tenant.getId(), EMAIL, RAW_PASSWORD, UserRole.ADMIN);

    assertUser(user, tenant, UserRole.ADMIN);
  }

  @Test
  void shouldHashPasswordBeforePersistingUser() {
    var tenant = createTenant();

    var user = createUserAndFlush(tenant.getId(), EMAIL, RAW_PASSWORD, UserRole.ADMIN);

    assertNotEquals(RAW_PASSWORD, user.getPasswordHash());
    assertFalse(user.getPasswordHash().isBlank());
    assertTrue(user.getPasswordHash().startsWith("$2"));
  }

  @Test
  void shouldThrowExceptionForDuplicateEmailWithinSameTenant() {
    var tenant = createTenant();

    createUserAndFlush(tenant.getId(), EMAIL, RAW_PASSWORD, UserRole.ADMIN);

    identityService.create(tenant.getId(), EMAIL, UUID.randomUUID().toString(), UserRole.OWNER);

    assertThrows(DataIntegrityViolationException.class, userRepository::flush);
  }

  @Test
  void shouldAllowSameEmailForDifferentTenants() {
    var tenantA = createTenant();
    var tenantB = createTenant();

    var user = createUserAndFlush(tenantA.getId(), EMAIL, RAW_PASSWORD, UserRole.ADMIN);
    var user2 = createUserAndFlush(tenantB.getId(), EMAIL, RAW_PASSWORD, UserRole.OWNER);

    assertUser(user, tenantA, UserRole.ADMIN);
    assertUser(user2, tenantB, UserRole.OWNER);
  }

  @Test
  void shouldNormalizeEmailBeforePersisting() {
    var tenant = createTenant();

    var user =
        createUserAndFlush(tenant.getId(), " Contact@email.com", RAW_PASSWORD, UserRole.ADMIN);

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

  private Tenant createTenant() {
    return tenantRepository.save(new Tenant("Don Corleone Pizzeria", Plan.MVP));
  }

  private User createUserAndFlush(UUID tenantId, String email, String rawPassword, UserRole role) {
    var user = identityService.create(tenantId, email, rawPassword, role);
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
