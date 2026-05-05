package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
  private final ProductRepository productRepository;

  public CatalogService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public Product createProduct(UUID tenantId, String name, BigDecimal price) {
    var product = new Product(tenantId, name, price);
    return productRepository.save(product);
  }

  public Optional<Product> findProduct(UUID tenantId, UUID productId) {
    return productRepository.findByIdAndTenantId(productId, tenantId);
  }

  public List<Product> listActiveProducts(UUID tenantId) {
    return productRepository.findActiveByTenantId(tenantId);
  }
}
