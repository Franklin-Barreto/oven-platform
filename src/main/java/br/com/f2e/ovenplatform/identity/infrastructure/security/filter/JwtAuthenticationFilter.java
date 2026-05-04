package br.com.f2e.ovenplatform.identity.infrastructure.security.filter;

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

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
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
      Claims claims = jwtService.parseClaims(token);
      String role = claims.get("role").toString();
      var authenticatedUser =
          new AuthenticatedUser(UUID.fromString(claims.getSubject()), UserRole.valueOf(role));
      var authenticated = getAuthenticated(authenticatedUser, role);
      securityContext.setAuthentication(authenticated);

    } catch (JwtException ex) {
      clearContextAndLog("Something wrong while trying to parse the token {}", ex);

    } catch (IllegalArgumentException ex) {
      clearContextAndLog("User had sent invalid arguments {}", ex);
    }
    filterChain.doFilter(request, response);
  }

  private static UsernamePasswordAuthenticationToken getAuthenticated(
      AuthenticatedUser authenticatedUser, String role) {
    return new UsernamePasswordAuthenticationToken(
        authenticatedUser, null, List.of(new SimpleGrantedAuthority(role)));
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
