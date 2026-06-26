package br.com.f2e.ovenplatform.identity.infrastructure.security;

import br.com.f2e.ovenplatform.identity.application.security.AuthenticatedPrincipal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUserDetails(UUID userId, String email, String password)
    implements UserDetails, AuthenticatedPrincipal {

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public @NonNull String getUsername() {
    return email;
  }
}
