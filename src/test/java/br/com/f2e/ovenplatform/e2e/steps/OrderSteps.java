package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.CreateOrderRequest;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderItemRequest;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderItemResponse;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderResponse;
import br.com.f2e.ovenplatform.shared.application.event.payload.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderPaymentStatus;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;

public class OrderSteps {

  private final E2eScenarioContext context;
  private final E2eApiClient api;

  public OrderSteps(E2eScenarioContext context, E2eApiClient api) {
    this.context = context;
    this.api = api;
  }

  @When("I create an order with {int} units of product {string}")
  public void createAnOrderWithUnitsOfProduct(int quantity, String productName) {
    var productResponse = context.productNamed(productName);
    assertThat(productResponse)
        .as("Product '%s' should exist in scenario context", productName)
        .isNotNull();

    var request =
        new CreateOrderRequest(
            List.of(new OrderItemRequest(productResponse.id(), quantity)),
            new PaymentInfo(PaymentMethod.CASH, OrderPaymentStatus.PAID));

    var response =
        api.authenticated()
            .body(request)
            .when()
            .post("/orders")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .as(OrderResponse.class);

    assertThat(response).isNotNull();
    context.setLastOrderResponse(response);
  }

  @Then("the order should be created successfully")
  public void theOrderShouldBeCreatedSuccessfully() {
    var order = lastOrder();

    assertThat(order.id()).isNotNull();
    assertThat(order.tenantId()).isEqualTo(context.tenantId());
    assertThat(order.items()).isNotEmpty();
  }

  @And("the order status should be {string}")
  public void theOrderStatusShouldBe(String expectedStatus) {
    assertThat(lastOrder().status()).isEqualTo(OrderStatus.valueOf(expectedStatus));
  }

  @And("the order total should be {bigdecimal}")
  public void theOrderTotalShouldBe(BigDecimal expectedTotal) {
    assertThat(lastOrder().totalAmount()).isEqualByComparingTo(expectedTotal);
  }

  @And("the order item should contain product {string}")
  public void theOrderItemShouldContainProduct(String productName) {
    assertThat(orderItemForProduct(productName).productName()).isEqualTo(productName);
  }

  @And("the order item unit price should be {bigdecimal}")
  public void theOrderItemUnitPriceShouldBe(BigDecimal expectedUnitPrice) {
    assertThat(singleOrderItem().unitPrice()).isEqualByComparingTo(expectedUnitPrice);
  }

  private OrderResponse lastOrder() {
    assertThat(context.lastOrderResponse()).as("Last order response should exist").isNotNull();
    return context.lastOrderResponse();
  }

  private OrderItemResponse singleOrderItem() {
    assertThat(lastOrder().items()).hasSize(1);
    return lastOrder().items().getFirst();
  }

  private OrderItemResponse orderItemForProduct(String productName) {
    return lastOrder().items().stream()
        .filter(item -> productName.equals(item.productName()))
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError("Order item for product '%s' not found".formatted(productName)));
  }
}
