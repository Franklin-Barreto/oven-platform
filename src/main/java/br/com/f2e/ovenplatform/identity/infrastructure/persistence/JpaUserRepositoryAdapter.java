package br.com.f2e.ovenplatform.identity.infrastructure.persistence;

import br.com.f2e.ovenplatform.identity.application.UserRepository;
import br.com.f2e.ovenplatform.identity.domain.User;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

  private final SpringDataUserRepository userRepository;

  public JpaUserRepositoryAdapter(SpringDataUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public User save(User user) {
    return userRepository.save(user);
  }
}
