package br.com.f2e.ovenplatform.catalog.infrastructure.persistence;

import br.com.f2e.ovenplatform.catalog.application.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProductRepositoryAdapter implements ProductRepository {

  private final SpringDataProductRepository repository;

  JpaProductRepositoryAdapter(SpringDataProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public Product save(Product product) {
    return repository.save(product);
  }

  @Override
  public Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId) {
    return repository.findByIdAndTenantId(id, tenantId);
  }

  @Override
  public List<Product> findActiveByTenantId(UUID tenantId) {
    return repository.findByTenantIdAndActiveTrue(tenantId);
  }
}
