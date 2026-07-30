package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ProductResult(
    UUID id,
    UUID tenantId,
    UUID categoryId,
    UUID imageId,
    URI imageUrl,
    String name,
    String description,
    BigDecimal price,
    boolean active) {

  public static ProductResult from(Product product, AvailableImage image) {
    if (!product.getImageId().equals(image.id())) {
      throw new IllegalArgumentException("Available image does not belong to product");
    }

    return new ProductResult(
        product.getId(),
        product.getTenantId(),
        product.getCategoryId(),
        product.getImageId(),
        image.publicUrl(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.isActive());
  }
}
