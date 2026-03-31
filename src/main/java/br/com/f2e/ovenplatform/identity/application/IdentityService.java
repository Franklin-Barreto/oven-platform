package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import java.util.UUID;
import org.springframework.stereotype.Service;

import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.normalizeEmail;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.identity.domain.validation.Preconditions.requireNotNull;

@Service
public class IdentityService {
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
}
