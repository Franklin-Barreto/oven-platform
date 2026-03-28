package br.com.f2e.ovenplatform.tenant.infrastructure.persistence;

import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTenantRepository extends JpaRepository<Tenant, UUID> {}
