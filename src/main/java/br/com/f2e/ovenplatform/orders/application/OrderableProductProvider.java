package br.com.f2e.ovenplatform.orders.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OrderableProductProvider {

  List<OrderableProduct> findOrderableProducts(UUID tenantId, Set<UUID> productIds);
}
