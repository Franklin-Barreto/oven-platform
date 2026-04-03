package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  User save(User user);

  Optional<User> findByIdAndTenantId(UUID tenantId, UUID id);
}
