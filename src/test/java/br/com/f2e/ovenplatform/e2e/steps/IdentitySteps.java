package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import br.com.f2e.ovenplatform.e2e.support.IdentityUserSeed;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginResponse;
import io.cucumber.java.en.Given;
import java.util.Set;

public class IdentitySteps {

  private final E2eScenarioContext context;
  private final IdentityUserSeed identityUserSeed;
  private final E2eApiClient api;

  public IdentitySteps(
      E2eScenarioContext context, IdentityUserSeed identityUserSeed, E2eApiClient api) {
    this.context = context;
    this.identityUserSeed = identityUserSeed;
    this.api = api;
  }

  @Given("an OWNER user exists for tenant {string}")
  public void ownerUserExistsForTenant(String tenantName) {
    var owner = identityUserSeed.seedOwnerForTenant(tenantName);

    context.setTenantId(owner.tenantId());
    context.setUserEmail(owner.email());
    context.setUserPassword(owner.password());
  }

  @Given("an ATTENDANT and KITCHEN user exists for tenant {string}")
  public void attendantAndKitchenUserExistsForTenant(String tenantName) {
    var user =
        identityUserSeed.seedStaffForTenant(
            tenantName, Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN));

    context.setTenantId(user.tenantId());
    context.setUserEmail(user.email());
    context.setUserPassword(user.password());
  }

  @Given("I am authenticated as that user")
  public void iAmAuthenticatedAsThatUser() {
    var request = new LoginRequest(context.tenantId(), context.userEmail(), context.userPassword());

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
