package br.com.f2e.ovenplatform.customer.infrastructure.persistence;

import br.com.f2e.ovenplatform.customer.domain.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCustomerRepository extends JpaRepository<Customer, UUID> {

  Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Customer> findByTenantIdAndNormalizedPhone(UUID tenantId, String normalizedPhone);

  List<Customer> findByTenantId(UUID tenantId);
}
