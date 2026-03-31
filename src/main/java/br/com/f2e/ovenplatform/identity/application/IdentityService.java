package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public IdentityService(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }

  public User create(UUID tenantId, String email, String rawPassword, UserRole role) {
    if (rawPassword == null || rawPassword.isBlank()) {
      throw new IllegalArgumentException("password must not be blank");
    }
    var passwordHash = passwordHasher.hash(rawPassword);
    return userRepository.save(new User(tenantId, email, passwordHash, role));
  }
}
