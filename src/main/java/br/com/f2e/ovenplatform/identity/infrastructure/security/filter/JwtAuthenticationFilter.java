package br.com.f2e.ovenplatform.identity.infrastructure.security.filter;

import br.com.f2e.ovenplatform.identity.application.TenantMembershipAuthenticationService;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  public static final String BEARER = "Bearer ";
  private final JwtService jwtService;
  private final TenantMembershipAuthenticationService membershipAuthenticationService;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      TenantMembershipAuthenticationService membershipAuthenticationService) {
    this.jwtService = jwtService;
    this.membershipAuthenticationService = membershipAuthenticationService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith(BEARER)) {
      filterChain.doFilter(request, response);
      return;
    }

    var securityContext = SecurityContextHolder.getContext();
    if (securityContext.getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    var token = authHeader.substring(BEARER.length());

    try {
      var claims = jwtService.parseClaims(token);
      var tenantId = UUID.fromString(claims.get("tenantId", String.class));
      var userId = UUID.fromString(claims.getSubject());
      var membership = membershipAuthenticationService.loadActiveMembership(userId, tenantId);
      var authenticatedUser =
          new AuthenticatedUser(
              membership.tenantId(),
              membership.userId(),
              membership.roles(),
              membership.permissions());
      var authenticated = getAuthenticated(authenticatedUser);
      securityContext.setAuthentication(authenticated);

    } catch (JwtException ex) {
      clearContextAndLog("Something wrong while trying to parse the token {}", ex);

    } catch (IllegalArgumentException ex) {
      clearContextAndLog("User had sent invalid arguments {}", ex);

    } catch (TenantAccessDeniedException | TenantMembershipInactiveException ex) {
      clearContextAndLog("Tenant membership could not be authenticated {}", ex);
    }
    filterChain.doFilter(request, response);
  }

  private static UsernamePasswordAuthenticationToken getAuthenticated(
      AuthenticatedUser authenticatedUser) {
    var authorities =
        authenticatedUser.permissions().stream()
            .map(permission -> new SimpleGrantedAuthority(permission.name()))
            .toList();
    return new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
  }

  private void clearContextAndLog(String message, Exception ex) {
    SecurityContextHolder.clearContext();
    LOGGER.warn(message, ex.getLocalizedMessage(), ex);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/auth/login");
  }
}
