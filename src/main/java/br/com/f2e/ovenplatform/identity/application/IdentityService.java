package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {
  private final UserRepository userRepository;

  public IdentityService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User create(UUID tenantId, String email, String passwordHash, UserRole role) {
    return userRepository.save(new User(tenantId, email, passwordHash, role));
  }
}
