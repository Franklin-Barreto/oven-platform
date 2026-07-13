package br.com.f2e.ovenplatform.orders.infrastructure.customer;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult.Address;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult.Line;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult.Location;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerDeliveryInfoLookupProviderTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID CUSTOMER_ID = UUID.fromString("c6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID CUSTOMER_ADDRESS_ID =
      UUID.fromString("d6210129-f1d5-4942-8d0a-b144e518aecc");

  @Test
  void shouldMapCustomerDeliveryInfoResultToOrdersDeliveryInfo() {
    var provider = new CustomerDeliveryInfoLookupProvider((_, _, _) -> result());

    var deliveryInfo =
        provider.findCustomerDeliveryInfo(TENANT_ID, CUSTOMER_ID, CUSTOMER_ADDRESS_ID);

    assertThat(deliveryInfo.customerId()).isEqualTo(CUSTOMER_ID);
    assertThat(deliveryInfo.customerName()).isEqualTo("Maria");
    assertThat(deliveryInfo.customerPhone()).isEqualTo("(11) 99999-8888");
    assertThat(deliveryInfo.address().addressId()).isEqualTo(CUSTOMER_ADDRESS_ID);
    assertThat(deliveryInfo.address().label()).isEqualTo("Home");
    assertThat(deliveryInfo.address().line().addressLine1()).isEqualTo("Rua das Flores");
    assertThat(deliveryInfo.address().line().number()).isEqualTo("123");
    assertThat(deliveryInfo.address().location().city()).isEqualTo("Sao Paulo");
    assertThat(deliveryInfo.address().reference()).isEqualTo("Portao azul");
  }

  private CustomerDeliveryInfoResult result() {
    return new CustomerDeliveryInfoResult(
        CUSTOMER_ID,
        "Maria",
        "(11) 99999-8888",
        new Address(
            CUSTOMER_ADDRESS_ID,
            "Home",
            new Line("Rua das Flores", "123", null),
            new Location("Centro", "Sao Paulo", "SP", "01000-000"),
            "Portao azul"));
  }
}
