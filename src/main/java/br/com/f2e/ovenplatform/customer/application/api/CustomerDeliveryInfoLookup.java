package br.com.f2e.ovenplatform.customer.application.api;

import java.util.UUID;

public interface CustomerDeliveryInfoLookup {

  CustomerDeliveryInfoResult findDeliveryInfo(
      UUID tenantId, UUID customerId, UUID customerAddressId);
}
