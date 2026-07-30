package br.com.f2e.ovenplatform.catalog.application.variant;

import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductVariantResultResolver {

  private final AvailableImageLookup availableImageLookup;

  public ProductVariantResultResolver(AvailableImageLookup availableImageLookup) {
    this.availableImageLookup = availableImageLookup;
  }

  public List<ProductVariantResult> resolve(UUID tenantId, List<ProductVariant> variants) {
    var imageIds =
        variants.stream()
            .map(ProductVariant::getImageId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    var imagesById =
        imageIds.isEmpty()
            ? Map.<UUID, AvailableImage>of()
            : availableImageLookup.getAvailableImages(tenantId, imageIds).stream()
                .collect(Collectors.toMap(AvailableImage::id, Function.identity()));

    return variants.stream()
        .map(
            variant -> {
              var image =
                  variant.getImageId() == null ? null : imagesById.get(variant.getImageId());
              var imageUrl = image == null ? null : image.publicUrl();

              return ProductVariantResult.from(variant, imageUrl);
            })
        .toList();
  }
}
