package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

  public ProductVariantService(
      ProductRepository productRepository,
      ProductVariantRepository variantRepository,
      AvailableImageLookup availableImageLookup) {
    this.productRepository = productRepository;
    this.variantRepository = variantRepository;
    this.availableImageLookup = availableImageLookup;
  }

  @Transactional
  public ProductVariant create(UUID tenantId, UUID productId, CreateProductVariantCommand command) {

    requireProduct(tenantId, productId);
    requireAvailableImageWhenPresent(tenantId, command.imageId());

    var nextPosition =
        variantRepository.findByTenantIdAndProductId(tenantId, productId).stream()
                .mapToInt(ProductVariant::getDisplayPosition)
                .max()
                .orElse(-1)
            + 1;

    var variant =
        new ProductVariant(
            productId, tenantId, command.imageId(), command.name(), command.price(), nextPosition);

    return variantRepository.save(variant);
  }

  @Transactional(readOnly = true)
  public List<ProductVariantResult> listVariants(UUID tenantId, UUID productId) {

    requireProduct(tenantId, productId);

    var productVariants = variantRepository.findByTenantIdAndProductId(tenantId, productId);

    if (productVariants.isEmpty()) {
      return Collections.emptyList();
    }

    var imageIds =
        productVariants.stream()
            .map(ProductVariant::getImageId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    var availableImages =
        availableImageLookup.getAvailableImages(tenantId, imageIds).stream()
            .collect(Collectors.toMap(AvailableImage::id, Function.identity()));

    return productVariants.stream()
        .map(
            variant -> {
              var availableImage = availableImages.get(variant.getImageId());
              var imageUrl = availableImage == null ? null : availableImage.publicUrl();

              return ProductVariantResult.from(variant, imageUrl);
            })
        .toList();
  }

  @Transactional
  public ProductVariantResult update(
      UUID tenantId, UUID productId, UUID variantId, UpdateProductVariantCommand command) {

    requireProduct(tenantId, productId);

    var variant =
        variantRepository
            .findByIdAndTenantIdAndProductId(variantId, tenantId, productId)
            .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_VARIANT_RESOURCE, variantId));

    var availableImage =
        command.imageId() == null
            ? null
            : availableImageLookup.getAvailableImage(tenantId, command.imageId());

    variant.updateDetails(command.imageId(), command.name(), command.price(), command.active());

    var imageUrl = availableImage == null ? null : availableImage.publicUrl();

    return ProductVariantResult.from(variant, imageUrl);
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

  private void requireAvailableImageWhenPresent(UUID tenantId, UUID imageId) {
    if (imageId != null) {
      availableImageLookup.getAvailableImage(tenantId, imageId);
    }
  }
}
