package br.com.f2e.ovenplatform.orders.infrastructure.catalog;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
import br.com.f2e.ovenplatform.catalog.application.api.ProductSelection;
import br.com.f2e.ovenplatform.orders.application.OrderableProduct;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.application.OrderableProductSelection;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CatalogOrderableProductProvider implements OrderableProductProvider {

  private final CatalogProductLookup catalogProductLookup;

  public CatalogOrderableProductProvider(CatalogProductLookup catalogProductLookup) {
    this.catalogProductLookup = catalogProductLookup;
  }

  @Override
  public List<OrderableProduct> findOrderableProducts(
          UUID tenantId, List<OrderableProductSelection> selections) {
    var productSelections =
            selections.stream()
                    .map(selection -> new ProductSelection(selection.productId(), selection.variantId()))
                    .toList();

    return catalogProductLookup.findSellableProducts(tenantId, productSelections).stream()
            .map(
                    sellableProduct ->
                            new OrderableProduct(
                                    sellableProduct.productId(),
                                    sellableProduct.productName(),
                                    sellableProduct.variantId(),
                                    sellableProduct.variantName(),
                                    sellableProduct.price()))
            .toList();
  }
}