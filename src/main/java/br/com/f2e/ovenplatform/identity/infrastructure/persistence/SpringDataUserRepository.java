package br.com.f2e.ovenplatform.identity.infrastructure.persistence;

import br.com.f2e.ovenplatform.identity.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<User, UUID> {}
