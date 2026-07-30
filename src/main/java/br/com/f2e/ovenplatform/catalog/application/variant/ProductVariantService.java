package br.com.f2e.ovenplatform.catalog.application.variant;

import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductVariantService {

  private static final String PRODUCT_RESOURCE = "Product";
  private static final String PRODUCT_VARIANT_RESOURCE = "ProductVariant";

  private final ProductRepository productRepository;
  private final ProductVariantRepository variantRepository;
  private final AvailableImageLookup availableImageLookup;
  private final ProductVariantResultResolver variantResultResolver;

  public ProductVariantService(
      ProductRepository productRepository,
      ProductVariantRepository variantRepository,
      AvailableImageLookup availableImageLookup,
      ProductVariantResultResolver variantResultResolver) {
    this.productRepository = productRepository;
    this.variantRepository = variantRepository;
    this.availableImageLookup = availableImageLookup;
    this.variantResultResolver = variantResultResolver;
  }

  @Transactional
  public ProductVariantResult create(
      UUID tenantId, UUID productId, CreateProductVariantCommand command) {

    requireProduct(tenantId, productId);
    var uri = requireURIImageWhenPresent(tenantId, command.imageId());

    var nextPosition =
        variantRepository.findByTenantIdAndProductId(tenantId, productId).stream()
                .mapToInt(ProductVariant::getDisplayPosition)
                .max()
                .orElse(-1)
            + 1;

    var variant =
        new ProductVariant(
            productId, tenantId, command.imageId(), command.name(), command.price(), nextPosition);

    return ProductVariantResult.from(variantRepository.save(variant), uri);
  }

  @Transactional(readOnly = true)
  public List<ProductVariantResult> listVariants(UUID tenantId, UUID productId) {

    requireProduct(tenantId, productId);

    var productVariants = variantRepository.findByTenantIdAndProductId(tenantId, productId);

    return variantResultResolver.resolve(tenantId, productVariants);
  }

  @Transactional
  public ProductVariantResult update(
      UUID tenantId, UUID productId, UUID variantId, UpdateProductVariantCommand command) {

    requireProduct(tenantId, productId);

    var variant = requireVariant(tenantId, productId, variantId);

    var availableImage =
        command.imageId() == null
            ? null
            : availableImageLookup.getAvailableImage(tenantId, command.imageId());

    variant.updateDetails(command.imageId(), command.name(), command.price());

    var imageUrl = availableImage == null ? null : availableImage.publicUrl();

    return ProductVariantResult.from(variant, imageUrl);
  }

  @Transactional
  public void changeStatus(UUID tenantId, UUID productId, UUID variantId, boolean active) {

    requireProduct(tenantId, productId);
    var variant = requireVariant(tenantId, productId, variantId);
    variant.changeStatusTo(active);
  }

  @Transactional
  public void reorder(UUID tenantId, UUID productId, ReorderProductVariantsCommand command) {

    requireProduct(tenantId, productId);

    var variants = variantRepository.findByTenantIdAndProductId(tenantId, productId);
    var requestedIds = command.variantIds();
    var uniqueRequestedIds = new HashSet<>(requestedIds);

    if (uniqueRequestedIds.size() != requestedIds.size()) {
      throw new IllegalArgumentException("variantIds must not contain duplicates");
    }

    var variantsById =
        variants.stream().collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

    if (requestedIds.size() != variants.size()
        || !variantsById.keySet().equals(uniqueRequestedIds)) {
      throw new IllegalArgumentException("variantIds must contain exactly all product variant ids");
    }

    for (var position = 0; position < requestedIds.size(); position++) {
      variantsById.get(requestedIds.get(position)).changeDisplayPosition(position);
    }
  }

  private void requireProduct(UUID tenantId, UUID productId) {
    productRepository
        .findByIdAndTenantId(productId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_RESOURCE, productId));
  }

  private ProductVariant requireVariant(UUID tenantId, UUID productId, UUID variantId) {
    return variantRepository
        .findByIdAndTenantIdAndProductId(variantId, tenantId, productId)
        .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_VARIANT_RESOURCE, variantId));
  }

  private URI requireURIImageWhenPresent(UUID tenantId, UUID imageId) {
    if (imageId != null) {
      return availableImageLookup.getAvailableImage(tenantId, imageId).publicUrl();
    }
    return null;
  }
}
