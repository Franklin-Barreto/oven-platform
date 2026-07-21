package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.application.security.AuthenticatedPrincipal;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final AccessTokenService jwtService;
  private final TenantMembershipRepository tenantMembershipRepository;

  public AuthService(
      AuthenticationManager authenticationManager,
      AccessTokenService jwtService,
      TenantMembershipRepository tenantMembershipRepository) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.tenantMembershipRepository = tenantMembershipRepository;
  }

  public String login(UUID tenantId, String email, String password) {

    var user = UsernamePasswordAuthenticationToken.unauthenticated(email, password);
    var authenticated = authenticationManager.authenticate(user);

    SecurityContextHolder.getContext().setAuthentication(authenticated);

    var loggedUser = getLoggedUser(authenticated);
    var tenantMembership = getTenantMembership(tenantId, loggedUser.userId());

    if (tenantMembership.getStatus() != TenantMembershipStatus.ACTIVE) {
      throw new TenantMembershipInactiveException();
    }
    return jwtService.generateToken(tenantId, loggedUser.userId());
  }

  private TenantMembership getTenantMembership(UUID tenantId, UUID userId) {
    return tenantMembershipRepository
        .findByUserIdAndTenantId(userId, tenantId)
        .orElseThrow(TenantAccessDeniedException::new);
  }

  private AuthenticatedPrincipal getLoggedUser(Authentication authenticated) {
    if (authenticated.getPrincipal() instanceof AuthenticatedPrincipal userDetails) {
      return userDetails;
    }

    throw new IllegalStateException("Authenticated principal is not an AuthenticatedPrincipal");
  }
}
