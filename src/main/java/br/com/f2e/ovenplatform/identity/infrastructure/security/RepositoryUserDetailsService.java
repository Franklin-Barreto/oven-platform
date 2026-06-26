package br.com.f2e.ovenplatform.identity.infrastructure.security;

import br.com.f2e.ovenplatform.identity.application.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RepositoryUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  public RepositoryUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String username) {
    var user =
        userRepository
            .findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new AuthenticatedUserDetails(user.getId(), user.getEmail(), user.getPassword());
  }
}
