package br.com.f2e.ovenplatform.identity.infrastructure.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.application.authentication.AuthenticatedTenantMembership;
import br.com.f2e.ovenplatform.identity.application.authentication.TenantMembershipAuthenticationService;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @InjectMocks private JwtAuthenticationFilter filter;

  @Mock private JwtService jwtService;
  @Mock private TenantMembershipAuthenticationService membershipAuthenticationService;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;
  @Mock Claims claims;

  private static final UUID SUBJECT = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final Set<TenantMembershipRole> ROLES =
      Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN);
  private static final Set<TenantPermission> PERMISSIONS =
      Set.of(
          TenantPermission.CATALOG_READ,
          TenantPermission.ORDER_CREATE,
          TenantPermission.KITCHEN_READ,
          TenantPermission.KITCHEN_OPERATE);

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    when(request.getRequestURI()).thenReturn("/api/protected");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSetAuthenticationWhenBearerTokenIsValid() throws ServletException, IOException {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");

    when(claims.getSubject()).thenReturn(SUBJECT.toString());
    when(claims.get("tenantId", String.class)).thenReturn(TENANT_ID.toString());
    when(jwtService.parseClaims("valid-token")).thenReturn(claims);
    when(membershipAuthenticationService.loadActiveMembership(SUBJECT, TENANT_ID))
        .thenReturn(new AuthenticatedTenantMembership(TENANT_ID, SUBJECT, ROLES, PERMISSIONS));

    filter.doFilter(request, response, filterChain);

    var authentication = SecurityContextHolder.getContext().getAuthentication();

    assertNotNull(authentication);
    assertTrue(authentication.isAuthenticated());
    assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());

    var authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

    assertEquals(TENANT_ID, authenticatedUser.tenantId());
    assertEquals(SUBJECT, authenticatedUser.userId());
    assertEquals(ROLES, authenticatedUser.roles());
    assertEquals(PERMISSIONS, authenticatedUser.permissions());
    assertEquals(
        Set.of(
            TenantPermission.CATALOG_READ.name(),
            TenantPermission.ORDER_CREATE.name(),
            TenantPermission.KITCHEN_READ.name(),
            TenantPermission.KITCHEN_OPERATE.name()),
        authentication.getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet()));

    verify(filterChain).doFilter(request, response);
    verify(jwtService).parseClaims("valid-token");
    verify(membershipAuthenticationService).loadActiveMembership(SUBJECT, TENANT_ID);
  }

  @Test
  void shouldNotAuthenticateWhenAuthorizationHeaderIsMissing()
      throws ServletException, IOException {

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(membershipAuthenticationService);
  }

  @Test
  void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid-token");
    when(jwtService.parseClaims("invalid-token")).thenThrow(new JwtException("Invalid token"));

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());

    verify(jwtService).parseClaims("invalid-token");
    verifyNoInteractions(membershipAuthenticationService);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotAuthenticateWhenMembershipIsAbsent() throws ServletException, IOException {
    prepareValidToken();
    when(membershipAuthenticationService.loadActiveMembership(SUBJECT, TENANT_ID))
        .thenThrow(new TenantAccessDeniedException());

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotAuthenticateWhenMembershipIsInactive() throws ServletException, IOException {
    prepareValidToken();
    when(membershipAuthenticationService.loadActiveMembership(SUBJECT, TENANT_ID))
        .thenThrow(new TenantMembershipInactiveException());

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotOverrideExistingAuthentication() throws ServletException, IOException {

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");

    var role = TenantMembershipRole.MANAGER;
    var permission = TenantPermission.KITCHEN_OPERATE;
    var authenticatedUser =
        new AuthenticatedUser(TENANT_ID, SUBJECT, Set.of(role), Set.of(permission));

    var existingAuthentication =
        new UsernamePasswordAuthenticationToken(
            authenticatedUser, null, List.of(new SimpleGrantedAuthority(permission.name())));

    SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

    filter.doFilter(request, response, filterChain);

    assertSame(existingAuthentication, SecurityContextHolder.getContext().getAuthentication());

    verifyNoInteractions(jwtService);
    verifyNoInteractions(membershipAuthenticationService);
    verify(filterChain).doFilter(request, response);
  }

  private void prepareValidToken() {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");
    when(claims.getSubject()).thenReturn(SUBJECT.toString());
    when(claims.get("tenantId", String.class)).thenReturn(TENANT_ID.toString());
    when(jwtService.parseClaims("valid-token")).thenReturn(claims);
  }
}
