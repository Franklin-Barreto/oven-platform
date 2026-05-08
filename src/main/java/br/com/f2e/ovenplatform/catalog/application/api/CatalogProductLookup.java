package br.com.f2e.ovenplatform.catalog.application.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CatalogProductLookup {

  List<SellableProduct> findSellableProducts(UUID tenantId, Set<UUID> productIds);
}
