package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.CreateOrderRequest;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderItemRequest;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderItemResponse;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderResponse;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentProcessingMode;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class OrderSteps {

  private final E2eScenarioContext context;
  private final E2eApiClient api;
  private Response orderListResponse;
  private OrderResponse orderDetailResponse;

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
            OrderServiceType.COUNTER,
            List.of(new OrderItemRequest(productResponse.id(), quantity)),
            new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID, PaymentProcessingMode.MANUAL));

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

  @When("I request the tenant order list")
  public void requestTenantOrderList() {
    orderListResponse = api.authenticated().when().get("/orders");
  }

  @Then("the order list request should succeed")
  public void orderListRequestShouldSucceed() {
    orderListResponse.then().statusCode(HttpStatus.OK.value());
  }

  @And("the order list should contain the created order")
  public void orderListShouldContainCreatedOrder() {
    var summaries = orderListResponse.jsonPath().<Map<String, Object>>getList("$");
    assertThat(summaries)
        .extracting(summary -> summary.get("id"))
        .contains(lastOrder().id().toString());
  }

  @And("the order summaries should not contain items")
  public void orderSummariesShouldNotContainItems() {
    var summaries = orderListResponse.jsonPath().<Map<String, Object>>getList("$");
    assertThat(summaries)
        .isNotEmpty()
        .allSatisfy(summary -> assertThat(summary).doesNotContainKey("items"));
  }

  @When("I request the created order details")
  public void requestCreatedOrderDetails() {
    orderDetailResponse =
        api.authenticated()
            .when()
            .get("/orders/{id}", lastOrder().id())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(OrderResponse.class);
  }

  @Then("the order details should contain product {string}")
  public void orderDetailsShouldContainProduct(String productName) {
    assertThat(orderDetailResponse.items())
        .extracting(OrderItemResponse::productName)
        .contains(productName);
  }

  @And("the detailed order item quantity should be {int}")
  public void detailedOrderItemQuantityShouldBe(int expectedQuantity) {
    assertThat(orderDetailResponse.items())
        .extracting(OrderItemResponse::quantity)
        .contains(expectedQuantity);
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
