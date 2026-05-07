package br.com.f2e.ovenplatform.orders.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.SpringDataProductRepository;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({OrderService.class, JpaOrderRepositoryAdapter.class})
@EnableJpaAuditing
class OrderServiceIntegrationTest {

  private static final BigDecimal UNIT_PRICE = new BigDecimal("35.40");

  @Autowired private OrderService orderService;
  @Autowired private SpringDataTenantRepository tenantRepository;
  @Autowired private SpringDataProductRepository productRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void shouldCreateOrder() {

    var tenant = createTenant();
    var order = orderService.createOrder(tenant.getId());

    assertThat(order.getTenantId()).isEqualTo(tenant.getId());
    assertThat(order.getId()).isNotNull();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(order.getItems()).isEmpty();
  }

  @Test
  void shouldSaveAndFindOrderWithItems() {
    var tenant = createTenant();
    var product = createProduct(tenant);

    var order = new Order(tenant.getId());
    var quantity = 2;

    order.addItem(product.getId(), quantity, UNIT_PRICE);

    var savedOrder = orderService.save(order);

    entityManager.flush();
    entityManager.clear();

    var foundOrder = orderService.findOrderWithItems(tenant.getId(), savedOrder.getId());

    assertThat(foundOrder).isPresent();

    var persistedOrder = foundOrder.get();

    var expectedPrice = "70.80";

    assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(persistedOrder.getTotalAmount()).isEqualByComparingTo(expectedPrice);
    assertThat(persistedOrder.getItems()).hasSize(1);

    var item = persistedOrder.getItems().getFirst();

    assertThat(item.getProductId()).isEqualTo(product.getId());
    assertThat(item.getQuantity()).isEqualTo(quantity);
    assertThat(item.getUnitPrice()).isEqualByComparingTo(UNIT_PRICE);
    assertThat(item.getSubtotal()).isEqualByComparingTo(expectedPrice);
  }

  @Test
  void shouldFindOrderByIdAndTenantId() {
    var tenant = createTenant();
    var savedOrder = orderService.createOrder(tenant.getId());

    entityManager.flush();
    entityManager.clear();

    var foundOrder = orderService.findOrder(tenant.getId(), savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getId()).isEqualTo(savedOrder.getId());
              assertThat(order.getTenantId()).isEqualTo(tenant.getId());
              assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
              assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            });
  }

  @Test
  void shouldReturnEmptyWhenOrderDoesNotExist() {

    var tenant = createTenant();
    var unknownOrderId = UUID.randomUUID();

    var order = orderService.findOrder(tenant.getId(), unknownOrderId);

    assertThat(order).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenOrderBelongsToAnotherTenant() {
    var tenant = createTenant();
    var anotherTenant = createTenant("Soprano Pizzeria");
    var order = orderService.createOrder(tenant.getId());

    entityManager.flush();
    entityManager.clear();

    assertThat(orderService.findOrder(anotherTenant.getId(), order.getId())).isEmpty();
  }

  @Test
  void shouldListOrdersByTenant() {

    UUID tenantId = createTenant().getId();
    orderService.createOrder(tenantId);

    entityManager.flush();
    entityManager.clear();

    var orders = orderService.listOrders(tenantId);

    assertThat(orders).hasSize(1).extracting(Order::getTenantId).containsOnly(tenantId);
  }

  @Test
  void shouldNotListOrdersFromAnotherTenant() {

    UUID tenantId = createTenant().getId();
    orderService.createOrder(tenantId);

    entityManager.flush();
    entityManager.clear();

    assertThat(orderService.listOrders(createTenant("La mama").getId())).isEmpty();
  }

  private Tenant createTenant() {
    return createTenant("Don Corleone Pizzeria");
  }

  private Tenant createTenant(String name) {
    return tenantRepository.save(new Tenant(name, Plan.MVP));
  }

  private Product createProduct(Tenant tenant) {
    return productRepository.save(new Product(tenant.getId(), "Pizza portuguesa", UNIT_PRICE));
  }
}
