package br.com.f2e.ovenplatform.e2e.support;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_HEADER;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.AUTHORIZATON_HEADER;
import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;

import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import io.cucumber.spring.ScenarioScope;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class E2eApiClient {

  private final E2eScenarioContext context;
  private final int port;

  public E2eApiClient(E2eScenarioContext context, @Value("${local.server.port}") int port) {
    this.context = context;
    this.port = port;
  }

  public RequestSpecification anonymous() {
    return baseRequest();
  }

  public RequestSpecification authenticated() {
    return baseRequest().header(AUTHORIZATON_HEADER, "Bearer " + context.accessToken());
  }

  private RequestSpecification baseRequest() {
    return given()
        .config(config().jsonConfig(jsonConfig().numberReturnType(BIG_DECIMAL)))
        .baseUri("http://localhost")
        .port(port)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .header(API_VERSION_HEADER, API_VERSION_VALUE);
  }
}
