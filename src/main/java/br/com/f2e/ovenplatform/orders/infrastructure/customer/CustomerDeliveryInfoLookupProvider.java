package br.com.f2e.ovenplatform.orders.infrastructure.customer;

import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoLookup;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfo;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfo.Address;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfo.Line;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfo.Location;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfoProvider;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerDeliveryInfoLookupProvider implements CustomerDeliveryInfoProvider {

  private final CustomerDeliveryInfoLookup customerDeliveryInfoLookup;

  public CustomerDeliveryInfoLookupProvider(CustomerDeliveryInfoLookup customerDeliveryInfoLookup) {
    this.customerDeliveryInfoLookup = customerDeliveryInfoLookup;
  }

  @Override
  public CustomerDeliveryInfo findCustomerDeliveryInfo(
      UUID tenantId, UUID customerId, UUID customerAddressId) {
    var deliveryInfoResult =
        customerDeliveryInfoLookup.findDeliveryInfo(tenantId, customerId, customerAddressId);

    return toCustomerDeliveryInfo(deliveryInfoResult);
  }

  private CustomerDeliveryInfo toCustomerDeliveryInfo(CustomerDeliveryInfoResult result) {
    var address = result.address();
    var line = address.line();
    var location = address.location();

    return new CustomerDeliveryInfo(
        result.customerId(),
        result.customerName(),
        result.customerPhone(),
        new Address(
            address.addressId(),
            address.label(),
            new Line(line.addressLine1(), line.number(), line.complement()),
            new Location(
                location.neighborhood(), location.city(), location.state(), location.postalCode()),
            address.reference()));
  }
}
