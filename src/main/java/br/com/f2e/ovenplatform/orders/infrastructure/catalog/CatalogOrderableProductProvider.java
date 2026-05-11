package br.com.f2e.ovenplatform.orders.infrastructure.catalog;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
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
    return catalogProductLookup.findSellableProducts(tenantId, productIds).stream()
        .map(
            sellableProduct ->
                new OrderableProduct(sellableProduct.productId(), sellableProduct.price()))
        .toList();
  }
}
