package br.com.f2e.ovenplatform.orders.infrastructure.catalog;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
import br.com.f2e.ovenplatform.catalog.application.api.ProductSelection;
import br.com.f2e.ovenplatform.orders.application.OrderableProduct;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogOrderableProductProvider implements OrderableProductProvider {

  private final CatalogProductLookup catalogProductLookup;

  public CatalogOrderableProductProvider(CatalogProductLookup catalogProductLookup) {
    this.catalogProductLookup = catalogProductLookup;
  }

  @Override
  public List<OrderableProduct> findOrderableProducts(UUID tenantId, Set<UUID> productIds) {
    var selections =
        productIds.stream().map(productId -> new ProductSelection(productId, null)).toList();

    return catalogProductLookup.findSellableProducts(tenantId, selections).stream()
        .map(
            sellableProduct ->
                new OrderableProduct(
                    sellableProduct.productId(),
                    sellableProduct.productName(),
                    sellableProduct.price()))
        .toList();
  }
}
