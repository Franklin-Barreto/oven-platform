package br.com.f2e.ovenplatform.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.customer.application.CreateCustomerAddressCommand;
import br.com.f2e.ovenplatform.customer.application.CreateCustomerCommand;
import br.com.f2e.ovenplatform.customer.application.CustomerDeliveryInfoLookupService;
import br.com.f2e.ovenplatform.customer.application.CustomerService;
import br.com.f2e.ovenplatform.customer.application.UpdateCustomerAddressCommand;
import br.com.f2e.ovenplatform.customer.application.UpdateCustomerCommand;
import br.com.f2e.ovenplatform.customer.infrastructure.persistence.JpaCustomerRepositoryAdapter;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.infrastructure.customer.CustomerDeliveryInfoLookupProvider;
import br.com.f2e.ovenplatform.orders.infrastructure.outbox.OutboxOrderCreatedEventPublisher;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.event.payload.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderPaymentStatus;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence.JpaOutboxEventRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({
  OrderService.class,
  JpaOrderRepositoryAdapter.class,
  OutboxService.class,
  JpaOutboxEventRepository.class,
  OutboxOrderCreatedEventPublisher.class,
  CustomerService.class,
  CustomerDeliveryInfoLookupService.class,
  JpaCustomerRepositoryAdapter.class,
  CustomerDeliveryInfoLookupProvider.class
})
class OrderCustomerSnapshotIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID PRODUCT_ID = UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecc");

  @Autowired private OrderService orderService;
  @Autowired private CustomerService customerService;
  @Autowired private SpringDataTenantRepository tenantRepository;

  @SuppressWarnings("unused")
  @MockitoBean
  private OrderableProductProvider orderableProductProvider;

  @SuppressWarnings("unused")
  @MockitoBean
  private Clock clock;

  @Test
  void shouldKeepDeliveryCustomerSnapshotAfterCustomerAndAddressChange() {
    var tenant = tenantRepository.save(new Tenant("Don Corleone Pizzeria", Plan.MVP));
    var customer =
        customerService.create(
            tenant.getId(), new CreateCustomerCommand("Maria", "(11) 99999-8888", null));
    var customerWithAddress =
        customerService.addAddress(tenant.getId(), customer.getId(), originalAddressCommand());
    var addressId = customerWithAddress.getAddresses().getFirst().getId();

    when(orderableProductProvider.findOrderableProducts(tenant.getId(), Set.of(PRODUCT_ID)))
        .thenReturn(
            List.of(new OrderableProduct(PRODUCT_ID, "Pizza Portuguesa", new BigDecimal("35.40"))));

    var command =
        new CreateOrderCommand(
            List.of(new CreateOrderItemCommand(PRODUCT_ID, 1)),
            new PaymentInfo(PaymentMethod.CASH, OrderPaymentStatus.PENDING),
            OrderServiceType.DELIVERY,
            customer.getId(),
            addressId);
    var savedOrder = orderService.createOrder(tenant.getId(), command);

    flushAndClear();

    customerService.update(
        tenant.getId(),
        customer.getId(),
        new UpdateCustomerCommand("Joana", "(21) 98888-7777", null));
    customerService.updateAddress(
        tenant.getId(), customer.getId(), addressId, updatedAddressCommand());

    flushAndClear();

    var snapshot =
        orderService
            .findOrderWithItems(tenant.getId(), savedOrder.getId())
            .orElseThrow()
            .getDeliveryCustomerSnapshot();
    var address = snapshot.getAddress();

    assertThat(snapshot.getCustomerId()).isEqualTo(customer.getId());
    assertThat(snapshot.getCustomerName()).isEqualTo("Maria");
    assertThat(snapshot.getCustomerPhone()).isEqualTo("(11) 99999-8888");
    assertThat(address.addressId()).isEqualTo(addressId);
    assertThat(address.label()).isEqualTo("Home");
    assertThat(address.line().addressLine1()).isEqualTo("Rua das Flores");
    assertThat(address.line().number()).isEqualTo("123");
    assertThat(address.location().neighborhood()).isEqualTo("Centro");
    assertThat(address.location().postalCode()).isEqualTo("01000-000");
  }

  private CreateCustomerAddressCommand originalAddressCommand() {
    return new CreateCustomerAddressCommand(
        "Home",
        "Rua das Flores",
        "123",
        null,
        "Centro",
        "Sao Paulo",
        "SP",
        "01000-000",
        "Portão azul");
  }

  private UpdateCustomerAddressCommand updatedAddressCommand() {
    return new UpdateCustomerAddressCommand(
        "Work",
        "Av Paulista",
        "1000",
        "Sala 1",
        "Bela Vista",
        "Sao Paulo",
        "SP",
        "01310-100",
        null);
  }
}
