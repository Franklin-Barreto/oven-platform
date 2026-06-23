package br.com.f2e.ovenplatform.identity.infrastructure.security.tenant;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantAccessInterceptorTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID ANOTHER_TENANT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  private TenantAccessInterceptor interceptor;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    interceptor = new TenantAccessInterceptor();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAllowRequestWhenTenantHeaderMatchesAuthenticatedTenant() {
    authenticateUserForTenant(TENANT_ID);

    var request = requestWithTenantHeader(TENANT_ID);

    var shouldContinue = interceptor.preHandle(request, response(), handler());

    assertTrue(shouldContinue);
  }

  @Test
  void shouldRejectRequestWhenTenantHeaderDiffersFromAuthenticatedTenant() {
    authenticateUserForTenant(TENANT_ID);

    var request = requestWithTenantHeader(ANOTHER_TENANT_ID);
    var handler = handler();
    var response = response();

    assertThrows(
        TenantAccessDeniedException.class, () -> interceptor.preHandle(request, response, handler));
  }

  @Test
  void shouldSkipValidationWhenTenantHeaderIsMissing() {
    authenticateUserForTenant(TENANT_ID);

    var request = new MockHttpServletRequest();

    assertDoesNotThrow(() -> interceptor.preHandle(request, response(), handler()));
  }

  @Test
  void shouldSkipValidationWhenAuthenticationIsMissing() {
    var request = requestWithTenantHeader(TENANT_ID);

    assertDoesNotThrow(() -> interceptor.preHandle(request, response(), handler()));
  }

  @Test
  void shouldSkipValidationWhenPrincipalIsNotAuthenticatedUser() {
    var authentication =
        new UsernamePasswordAuthenticationToken(
            "john@email.com",
            null,
            List.of(new SimpleGrantedAuthority(TenantMembershipRole.MEMBER.name())));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    var request = requestWithTenantHeader(TENANT_ID);

    assertDoesNotThrow(() -> interceptor.preHandle(request, response(), handler()));
  }

  private static MockHttpServletRequest requestWithTenantHeader(UUID tenantId) {
    var request = new MockHttpServletRequest();
    request.addHeader(TENANT_ID_HEADER, tenantId.toString());
    return request;
  }

  private static void authenticateUserForTenant(UUID tenantId) {
    var role = TenantMembershipRole.MEMBER;
    var authenticatedUser = new AuthenticatedUser(tenantId, USER_ID, role);

    var authentication =
        new UsernamePasswordAuthenticationToken(
            authenticatedUser, null, List.of(new SimpleGrantedAuthority(role.name())));

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private static MockHttpServletResponse response() {
    return new MockHttpServletResponse();
  }

  private static Object handler() {
    return new Object();
  }
}
