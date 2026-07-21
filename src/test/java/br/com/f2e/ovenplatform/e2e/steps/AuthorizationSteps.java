package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpStatus;

public class AuthorizationSteps {

  private final E2eScenarioContext context;
  private final E2eApiClient api;

  public AuthorizationSteps(E2eScenarioContext context, E2eApiClient api) {
    this.context = context;
    this.api = api;
  }

  @When("I list the tenant orders")
  public void listTenantOrders() {
    context.setLastResponseStatus(api.authenticated().when().get("/orders").statusCode());
  }

  @When("I list the kitchen tickets")
  public void listKitchenTickets() {
    context.setLastResponseStatus(api.authenticated().when().get("/kitchen/tickets").statusCode());
  }

  @Then("the request should be allowed")
  public void requestShouldBeAllowed() {
    assertThat(context.lastResponseStatus()).isEqualTo(HttpStatus.OK.value());
  }
}
