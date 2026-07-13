package br.com.f2e.ovenplatform.orders.application;

import java.util.UUID;

public interface CustomerDeliveryInfoProvider {

  CustomerDeliveryInfo findCustomerDeliveryInfo(
      UUID tenantId, UUID customerId, UUID customerAddressId);
}
