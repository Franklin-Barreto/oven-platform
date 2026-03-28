package br.com.f2e.ovenplatform.tenant.application;

import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

  private final TenantRepository repository;

  @Autowired
  public TenantService(TenantRepository repository) {
    this.repository = repository;
  }

  public Tenant create(String name, Plan plan) {
    var tenant = new Tenant(name, plan);
    return repository.save(tenant);
  }
}
