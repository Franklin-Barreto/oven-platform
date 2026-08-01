package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
import br.com.f2e.ovenplatform.catalog.application.api.ProductSelection;
import br.com.f2e.ovenplatform.catalog.application.api.SellableProduct;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantRepository;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CatalogProductLookupService implements CatalogProductLookup {

  private final ProductRepository productRepository;
  private final ProductVariantRepository variantRepository;

  public CatalogProductLookupService(
      ProductRepository productRepository, ProductVariantRepository variantRepository) {
    this.productRepository = productRepository;
    this.variantRepository = variantRepository;
  }

  @Override
  public List<SellableProduct> findSellableProducts(
      UUID tenantId, List<ProductSelection> selections) {
    var sellableProducts = new ArrayList<SellableProduct>();
    var productIds =
        selections.stream().map(ProductSelection::productId).collect(Collectors.toSet());

    var products = productRepository.findActiveByTenantIdAndIdIn(tenantId, productIds);

    var productVariants = variantRepository.findByTenantIdAndProductIds(tenantId, productIds);

    var productsById =
        products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));

    var variantsById =
        productVariants.stream()
            .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

    var productIdsWithVariants =
        productVariants.stream().map(ProductVariant::getProductId).collect(Collectors.toSet());

    for (var selection : selections) {
      resolveSelection(selection, productsById, productIdsWithVariants, variantsById)
          .ifPresent(sellableProducts::add);
    }

    return sellableProducts;
  }

  private static Optional<SellableProduct> resolveSelection(
      ProductSelection selection,
      Map<UUID, Product> productsById,
      Set<UUID> productIdsWithVariants,
      Map<UUID, ProductVariant> variantsById) {

    var product = productsById.get(selection.productId());

    if (product == null) {
      return Optional.empty();
    }

    if (selection.variantId() == null) {
      if (productIdsWithVariants.contains(product.getId())) {
        return Optional.empty();
      }

      return Optional.of(
          new SellableProduct(product.getId(), product.getName(), product.getPrice()));
    }

    var variant = variantsById.get(selection.variantId());

    if (variant == null || !variant.isActive() || !variant.getProductId().equals(product.getId())) {
      return Optional.empty();
    }

    return Optional.of(
        new SellableProduct(
            product.getId(),
            product.getName(),
            variant.getId(),
            variant.getName(),
            variant.getPrice()));
  }
}
