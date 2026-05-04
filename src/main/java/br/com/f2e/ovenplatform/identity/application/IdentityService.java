package br.com.f2e.ovenplatform.identity.application;

import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.normalizeEmail;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class IdentityService implements UserDetailsService {
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public IdentityService(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }

  public User create(UUID tenantId, String email, String rawPassword, UserRole role) {
    requireNotNull(tenantId, "tenantId");
    requireNotBlank(rawPassword, "rawPassword");
    requireNotNull(role, "role");

    var passwordHash = passwordHasher.hash(rawPassword);
    return userRepository.save(new User(tenantId, normalizeEmail(email), passwordHash, role));
  }

  public User findByIdAndTenantId(UUID id, UUID tenantId) {
    return userRepository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new NoSuchElementException("User"));
  }

  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String username)
      throws UsernameNotFoundException {
    return userRepository
        .findByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
