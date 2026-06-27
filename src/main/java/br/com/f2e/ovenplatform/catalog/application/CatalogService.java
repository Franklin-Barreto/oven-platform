package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

  private static final String CATEGORY_RESOURCE = "Category";
  private static final String PRODUCT_RESOURCE = "Product";

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  public CatalogService(
      ProductRepository productRepository, CategoryRepository categoryRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
  }

  public Product createProduct(
      UUID tenantId, UUID categoryId, String name, String description, BigDecimal price) {
    requireActiveCategory(tenantId, categoryId);
    var product = new Product(tenantId, categoryId, name, description, price);
    return productRepository.save(product);
  }

  public Optional<Product> findProduct(UUID tenantId, UUID productId) {
    return productRepository.findByIdAndTenantId(productId, tenantId);
  }

  public Product getProduct(UUID tenantId, UUID productId) {
    return findRequiredProduct(tenantId, productId);
  }

  public List<Product> listActiveProducts(UUID tenantId) {
    return productRepository.findActiveByTenantId(tenantId);
  }

  public Product update(
      UUID tenantId,
      UUID productId,
      UUID categoryId,
      String name,
      String description,
      BigDecimal price,
      boolean active) {
    requireActiveCategory(tenantId, categoryId);
    var product = findRequiredProduct(tenantId, productId);
    product.changeCategory(categoryId);
    product.rename(name);
    product.changeDescription(description);
    product.changePrice(price);
    if (active) {
      product.activate();
    } else {
      product.deactivate();
    }
    return productRepository.save(product);
  }

  public void deactivate(UUID tenantId, UUID productId) {
    var product = findRequiredProduct(tenantId, productId);
    product.deactivate();
    productRepository.save(product);
  }

  private Product findRequiredProduct(UUID tenantId, UUID productId) {
    return productRepository
        .findByIdAndTenantId(productId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_RESOURCE, productId));
  }

  private void requireActiveCategory(UUID tenantId, UUID categoryId) {
    var category =
        categoryRepository
            .findByIdAndTenantId(categoryId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_RESOURCE, categoryId));

    if (!category.isActive()) {
      throw new ResourceNotFoundException(CATEGORY_RESOURCE, categoryId);
    }
  }
}
