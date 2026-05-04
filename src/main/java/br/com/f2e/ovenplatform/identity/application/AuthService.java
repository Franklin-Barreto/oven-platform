package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final AccessTokenService jwtService;

  public AuthService(AuthenticationManager authenticationManager, AccessTokenService jwtService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  public String login(String email, String password) {
    var user = UsernamePasswordAuthenticationToken.unauthenticated(email, password);
    var authenticated = authenticationManager.authenticate(user);
    SecurityContextHolder.getContext().setAuthentication(authenticated);
    var loggedUser = getLoggedUser(authenticated);
    return jwtService.generateToken(loggedUser.getId(), loggedUser.getRole().name());
  }

  private static User getLoggedUser(Authentication authenticated) {
    if (authenticated.getPrincipal() instanceof User user) {
      return user;
    }

    throw new IllegalStateException("Authenticated principal is not a User");
  }
}
