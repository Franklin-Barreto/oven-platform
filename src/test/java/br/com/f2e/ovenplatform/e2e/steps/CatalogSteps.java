package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.catalog.infrastructure.web.CategoryResponse;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.CreateCategoryRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.CreateProductRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.ProductResponse;
import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;

public class CatalogSteps {

  private final E2eScenarioContext context;
  private final E2eApiClient api;

  public CatalogSteps(E2eScenarioContext context, E2eApiClient api) {
    this.context = context;
    this.api = api;
  }

  @Given("a category named {string} exists")
  public void categoryNameExists(String categoryName) {
    var request = new CreateCategoryRequest(categoryName);

    var response =
        api.authenticated()
            .body(request)
            .when()
            .post("/categories")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .as(CategoryResponse.class);

    assertThat(response).isNotNull();

    context.addCategory(response);
  }

  @And("a product named {string} priced at {bigdecimal} exists in category {string}")
  public void productExistsInCategory(String productName, BigDecimal price, String categoryName) {

    var category = context.categoryNamed(categoryName);
    var request = new CreateProductRequest(category.id(), productName, "Delicious pizza", price);

    var response =
        api.authenticated()
            .body(request)
            .when()
            .post("/products")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .as(ProductResponse.class);

    assertThat(response).isNotNull();
    context.addProduct(response);
  }
}
