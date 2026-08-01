package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.category.CategoryRepository;
import br.com.f2e.ovenplatform.catalog.application.option.OptionRepository;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupRepository;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantRepository;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResultResolver;
import br.com.f2e.ovenplatform.catalog.domain.Option;
import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private final ProductVariantRepository variantRepository;
  private final ProductVariantResultResolver variantResultResolver;
  private final OptionGroupRepository optionGroupRepository;
  private final OptionRepository optionRepository;

  public CatalogService(
      ProductRepository productRepository,
      CategoryRepository categoryRepository,
      AvailableImageLookup availableImageLookup,
      ProductVariantRepository variantRepository,
      ProductVariantResultResolver variantResultResolver,
      OptionGroupRepository optionGroupRepository,
      OptionRepository optionRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.availableImageLookup = availableImageLookup;
    this.variantRepository = variantRepository;
    this.variantResultResolver = variantResultResolver;
    this.optionGroupRepository = optionGroupRepository;
    this.optionRepository = optionRepository;
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

  public ProductDetailResult getProduct(UUID tenantId, UUID productId) {
    var product = findRequiredProduct(tenantId, productId);
    var variants = variantRepository.findByTenantIdAndProductId(tenantId, productId);
    var allVariantResults =
        variants.stream().map(variant -> ProductVariantResult.from(variant, null)).toList();
    var activeVariants = variants.stream().filter(ProductVariant::isActive).toList();
    var resolvedActiveVariants = variantResultResolver.resolve(tenantId, activeVariants);

    List<ProductOptionGroupDetailResult> optionGroups =
        optionGroupRepository.findByTenantIdAndProductId(tenantId, productId).stream()
            .filter(OptionGroup::isActive)
            .map(optionGroup -> toProductOptionGroupDetail(tenantId, optionGroup))
            .toList();

    return ProductDetailResult.from(
        toResult(product), allVariantResults, resolvedActiveVariants, optionGroups);
  }

  public List<ProductSummaryResult> listActiveProducts(UUID tenantId) {
    var products = productRepository.findActiveByTenantId(tenantId);
    var imageIds = products.stream().map(Product::getImageId).collect(Collectors.toSet());
    var imagesById = getAvailableImagesById(tenantId, imageIds);

    var variantsByProductId =
        variantRepository
            .findByTenantIdAndProductIds(
                tenantId, products.stream().map(Product::getId).collect(Collectors.toSet()))
            .stream()
            .map(variant -> ProductVariantResult.from(variant, null))
            .collect(Collectors.groupingBy(ProductVariantResult::productId));

    return products.stream()
        .map(
            product ->
                ProductSummaryResult.from(
                    ProductResult.from(product, imagesById.get(product.getImageId())),
                    variantsByProductId.getOrDefault(product.getId(), List.of())))
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

  private ProductOptionGroupDetailResult toProductOptionGroupDetail(
      UUID tenantId, OptionGroup optionGroup) {
    List<ProductOptionDetailResult> options =
        optionRepository.findByTenantIdAndOptionGroupId(tenantId, optionGroup.getId()).stream()
            .filter(Option::isActive)
            .map(
                option ->
                    new ProductOptionDetailResult(
                        option.getId(),
                        option.getName(),
                        option.getPriceAdjustment(),
                        option.getDisplayPosition()))
            .toList();

    return new ProductOptionGroupDetailResult(
        optionGroup.getId(),
        optionGroup.getName(),
        optionGroup.getMinimumSelections(),
        optionGroup.getMaximumSelections(),
        optionGroup.getDisplayPosition(),
        options);
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

  private Map<UUID, AvailableImage> getAvailableImagesById(UUID tenantId, Set<UUID> imageIds) {

    if (imageIds.isEmpty()) {
      return Map.of();
    }

    return availableImageLookup.getAvailableImages(tenantId, imageIds).stream()
        .collect(Collectors.toMap(AvailableImage::id, Function.identity()));
  }
}
