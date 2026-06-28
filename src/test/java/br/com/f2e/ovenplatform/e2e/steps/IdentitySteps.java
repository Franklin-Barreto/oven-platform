package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import br.com.f2e.ovenplatform.e2e.support.OwnerUserSeed;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginResponse;
import io.cucumber.java.en.Given;

public class IdentitySteps {

  private final E2eScenarioContext context;
  private final OwnerUserSeed ownerUserSeed;
  private final E2eApiClient api;

  public IdentitySteps(E2eScenarioContext context, OwnerUserSeed ownerUserSeed, E2eApiClient api) {
    this.context = context;
    this.ownerUserSeed = ownerUserSeed;
    this.api = api;
  }

  @Given("an OWNER user exists for tenant {string}")
  public void ownerUserExistsForTenant(String tenantName) {
    var owner = ownerUserSeed.seedOwnerForTenant(tenantName);

    context.setTenantId(owner.tenantId());
    context.setOwnerEmail(owner.email());
    context.setOwnerPassword(owner.password());
  }

  @Given("I am authenticated as that user")
  public void iAmAuthenticatedAsThatUser() {
    var request =
        new LoginRequest(context.tenantId(), context.ownerEmail(), context.ownerPassword());

    var response =
        api.anonymous()
            .body(request)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .as(LoginResponse.class);

    assertThat(response.token()).isNotBlank();
    context.setAccessToken(response.token());
  }
}
