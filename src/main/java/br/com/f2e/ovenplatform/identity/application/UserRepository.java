package br.com.f2e.ovenplatform.identity.application;

import br.com.f2e.ovenplatform.identity.domain.User;

public interface UserRepository {

  User save(User user);
}
