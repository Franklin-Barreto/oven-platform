package br.com.f2e.ovenplatform.tenant.infrastructure.persistence;

import br.com.f2e.ovenplatform.tenant.application.TenantRepository;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTenantRepositoryAdapter implements TenantRepository {

  private final SpringDataTenantRepository repository;

  public JpaTenantRepositoryAdapter(SpringDataTenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public Tenant save(Tenant tenant) {
    return repository.save(tenant);
  }

  @Override
  public Optional<Tenant> findById(UUID id) {
    return repository.findById(id);
  }
}
