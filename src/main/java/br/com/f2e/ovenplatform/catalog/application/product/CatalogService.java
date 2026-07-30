package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.category.CategoryRepository;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

  private static final String CATEGORY_RESOURCE = "Category";
  private static final String PRODUCT_RESOURCE = "Product";

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final AvailableImageLookup availableImageLookup;

  public CatalogService(
      ProductRepository productRepository,
      CategoryRepository categoryRepository,
      AvailableImageLookup availableImageLookup) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.availableImageLookup = availableImageLookup;
  }

  public ProductResult createProduct(UUID tenantId, CreateProductCommand command) {

    requireActiveCategory(tenantId, command.categoryId());
    var availableImage = availableImageLookup.getAvailableImage(tenantId, command.imageId());

    var product =
        new Product(
            tenantId,
            command.categoryId(),
            command.imageId(),
            command.name(),
            command.description(),
            command.price());

    return ProductResult.from(productRepository.save(product), availableImage);
  }

  public Optional<ProductResult> findProduct(UUID tenantId, UUID productId) {
    return productRepository.findByIdAndTenantId(productId, tenantId).map(this::toResult);
  }

  public ProductResult getProduct(UUID tenantId, UUID productId) {
    return toResult(findRequiredProduct(tenantId, productId));
  }

  public List<ProductResult> listActiveProducts(UUID tenantId) {
    var products = productRepository.findActiveByTenantId(tenantId);
    var imageIds = products.stream().map(Product::getImageId).collect(Collectors.toSet());
    var imagesById =
        availableImageLookup.getAvailableImages(tenantId, imageIds).stream()
            .collect(Collectors.toMap(AvailableImage::id, Function.identity()));

    return products.stream()
        .map(product -> ProductResult.from(product, imagesById.get(product.getImageId())))
        .toList();
  }

  public ProductResult update(UUID tenantId, UUID productId, UpdateProductCommand command) {

    requireActiveCategory(tenantId, command.categoryId());
    var availableImage = availableImageLookup.getAvailableImage(tenantId, command.imageId());

    var product = findRequiredProduct(tenantId, productId);
    product.updateDetails(
        command.categoryId(),
        command.imageId(),
        command.name(),
        command.description(),
        command.price(),
        command.active());

    return ProductResult.from(productRepository.save(product), availableImage);
  }

  public void deactivate(UUID tenantId, UUID productId) {
    var product = findRequiredProduct(tenantId, productId);
    product.deactivate();
    productRepository.save(product);
  }

  private ProductResult toResult(Product product) {
    var image = availableImageLookup.getAvailableImage(product.getTenantId(), product.getImageId());

    return ProductResult.from(product, image);
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
