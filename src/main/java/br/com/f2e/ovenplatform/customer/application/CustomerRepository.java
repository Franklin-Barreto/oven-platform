package br.com.f2e.ovenplatform.customer.application;

import br.com.f2e.ovenplatform.customer.domain.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

  Customer save(Customer customer);

  Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Customer> findByTenantIdAndNormalizedPhone(UUID tenantId, String normalizedPhone);

  List<Customer> findByTenantId(UUID tenantId);
}
