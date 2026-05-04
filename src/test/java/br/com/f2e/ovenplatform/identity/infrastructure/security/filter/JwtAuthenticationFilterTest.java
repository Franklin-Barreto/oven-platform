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

import br.com.f2e.ovenplatform.identity.domain.UserRole;
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
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  private JwtAuthenticationFilter filter;

  @Mock private JwtService jwtService;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;
  @Mock Claims claims;

  private final UUID subject = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    filter = new JwtAuthenticationFilter(jwtService);
    when(request.getRequestURI()).thenReturn("/api/protected");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSetAuthenticationWhenBearerTokenIsValid() throws ServletException, IOException {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");

    when(claims.getSubject()).thenReturn(subject.toString());
    when(claims.get("role")).thenReturn(UserRole.MEMBER.name());
    when(jwtService.parseClaims("valid-token")).thenReturn(claims);

    filter.doFilter(request, response, filterChain);

    var authentication = SecurityContextHolder.getContext().getAuthentication();

    assertNotNull(authentication);
    assertTrue(authentication.isAuthenticated());
    assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());

    var authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

    assertEquals(subject, authenticatedUser.id());
    assertEquals(UserRole.MEMBER, authenticatedUser.role());

    assertTrue(
        authentication.getAuthorities().stream()
            .anyMatch(
                authority -> Objects.equals(authority.getAuthority(), UserRole.MEMBER.name())));

    verify(filterChain).doFilter(request, response);
    verify(jwtService).parseClaims("valid-token");
  }

  @Test
  void shouldNotAuthenticateWhenAuthorizationHeaderIsMissing()
      throws ServletException, IOException {

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(jwtService);
  }

  @Test
  void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid-token");
    when(jwtService.parseClaims("invalid-token")).thenThrow(new JwtException("Invalid token"));

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());

    verify(jwtService).parseClaims("invalid-token");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotOverrideExistingAuthentication() throws ServletException, IOException {

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid-token");

    var role = UserRole.ADMIN;
    var authenticatedUser = new AuthenticatedUser(subject, role);

    var existingAuthentication =
        new UsernamePasswordAuthenticationToken(
            authenticatedUser, null, List.of(new SimpleGrantedAuthority(role.name())));

    SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

    filter.doFilter(request, response, filterChain);

    assertSame(existingAuthentication, SecurityContextHolder.getContext().getAuthentication());

    verifyNoInteractions(jwtService);
    verify(filterChain).doFilter(request, response);
  }
}
