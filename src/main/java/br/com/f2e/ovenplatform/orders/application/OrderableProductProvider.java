package br.com.f2e.ovenplatform.orders.application;

import java.util.List;
import java.util.UUID;

public interface OrderableProductProvider {

  List<OrderableProduct> findOrderableProducts(
      UUID tenantId, List<OrderableProductSelection> selections);
}
