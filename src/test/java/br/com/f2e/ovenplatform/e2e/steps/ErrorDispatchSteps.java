package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpStatus;

public class ErrorDispatchSteps {

  private final E2eScenarioContext context;
  private final E2eApiClient api;
  private String accessTokenBeforeFailure;

  public ErrorDispatchSteps(E2eScenarioContext context, E2eApiClient api) {
    this.context = context;
    this.api = api;
  }

  @When("I request a protected endpoint that fails unexpectedly")
  public void requestProtectedEndpointThatFailsUnexpectedly() {
    accessTokenBeforeFailure = context.accessToken();
    context.setLastResponseStatus(
        api.authenticated().when().get("/test/internal-failure").statusCode());
  }

  @Then("the response should be an internal server error")
  public void responseShouldBeInternalServerError() {
    assertThat(context.lastResponseStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  @Then("the same access token should remain valid")
  public void sameAccessTokenShouldRemainValid() {
    assertThat(context.accessToken()).isEqualTo(accessTokenBeforeFailure);
    api.authenticated().when().get("/orders").then().statusCode(HttpStatus.OK.value());
  }
}
