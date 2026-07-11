package br.com.f2e.ovenplatform.customer.infrastructure.persistence;

import br.com.f2e.ovenplatform.customer.application.CustomerRepository;
import br.com.f2e.ovenplatform.customer.domain.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCustomerRepositoryAdapter implements CustomerRepository {

  private final SpringDataCustomerRepository repository;

  JpaCustomerRepositoryAdapter(SpringDataCustomerRepository repository) {
    this.repository = repository;
  }

  @Override
  public Customer save(Customer customer) {
    return repository.save(customer);
  }

  @Override
  public Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId) {
    return repository.findByIdAndTenantId(id, tenantId);
  }

  @Override
  public Optional<Customer> findByTenantIdAndNormalizedPhone(
      UUID tenantId, String normalizedPhone) {
    return repository.findByTenantIdAndNormalizedPhone(tenantId, normalizedPhone);
  }

  @Override
  public List<Customer> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }
}
