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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityService implements UserDetailsService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public IdentityService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User create(UUID tenantId, String email, String rawPassword, UserRole role) {
    requireNotNull(tenantId, "tenantId");
    requireNotBlank(rawPassword, "rawPassword");
    requireNotNull(role, "role");

    var passwordHash = passwordEncoder.encode(rawPassword);
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
