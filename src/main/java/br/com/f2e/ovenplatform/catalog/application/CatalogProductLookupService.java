package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
import br.com.f2e.ovenplatform.catalog.application.api.SellableProduct;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CatalogProductLookupService implements CatalogProductLookup {

  private final ProductRepository repository;

  public CatalogProductLookupService(ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SellableProduct> findSellableProducts(UUID tenantId, Set<UUID> productIds) {

    return repository.findActiveByTenantIdAndIdIn(tenantId, productIds).stream()
        .map(product -> new SellableProduct(product.getId(), product.getPrice()))
        .toList();
  }
}
