package br.com.f2e.ovenplatform.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.f2e.ovenplatform.identity.domain.Status;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import br.com.f2e.ovenplatform.identity.infrastructure.persistence.SpringDataUserRepository;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IdentityServiceIntegrationTest {

  public static final String EMAIL = "contact@email.com";
  @Autowired private IdentityService identityService;
  @Autowired private SpringDataTenantRepository tenantRepository;
  @Autowired private SpringDataUserRepository userRepository;

  @Test
  void shouldCreateUserSuccessfully() {
    var tenant = createTenant();
    var user = createUserAndFlush(tenant.getId(), EMAIL, UserRole.ADMIN);
    assertsUser(user, tenant, UserRole.ADMIN);
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
    assertsUser(user, tenantA, UserRole.ADMIN);
    assertsUser(user2, tenantB, UserRole.OWNER);
  }

  @Test
  void shouldNormalizeEmailBeforePersisting() {
    var tenant = createTenant();
    var user = createUserAndFlush(tenant.getId(), " Contact@email.com", UserRole.ADMIN);
    assertEquals(EMAIL, user.getEmail());
    assertEquals(tenant.getId(), user.getTenantId());
    assertEquals(UserRole.ADMIN, user.getRole());
    assertEquals(Status.ACTIVE, user.getStatus());
  }

  @Test
  void shouldThrowExceptionWhenEmailIsInvalid() {
    var tenant = createTenant();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            identityService.create(
                tenant.getId(),
                "invalidEmailOutlook.com",
                UUID.randomUUID().toString(),
                UserRole.MEMBER));
  }

  @Test
  void shouldThrowExceptionWhenPasswordHashIsBlank() {
    var tenant = createTenant();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            identityService.create(tenant.getId(), EMAIL, "", UserRole.MEMBER));
  }

  private Tenant createTenant() {
    return tenantRepository.save(new Tenant("Don Corleone Pizzeria", Plan.MVP));
  }

  private User createUserAndFlush(UUID tenantId, String email, UserRole role) {
    var user = identityService.create(tenantId, email, UUID.randomUUID().toString(), role);
    userRepository.flush();
    return user;
  }

  private static void assertsUser(User user, Tenant tenantA, UserRole expectedRole) {
    assertEquals(EMAIL, user.getEmail());
    assertEquals(tenantA.getId(), user.getTenantId());
    assertEquals(expectedRole, user.getRole());
    assertEquals(Status.ACTIVE, user.getStatus());
  }
}
