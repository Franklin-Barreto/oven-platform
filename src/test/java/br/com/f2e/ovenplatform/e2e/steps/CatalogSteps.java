package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.catalog.infrastructure.web.CategoryResponse;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.CreateCategoryRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.CreateProductRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.ProductResponse;
import br.com.f2e.ovenplatform.e2e.context.E2eScenarioContext;
import br.com.f2e.ovenplatform.e2e.support.E2eApiClient;
import br.com.f2e.ovenplatform.media.application.StoredImageRepository;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CatalogSteps {

  private final E2eScenarioContext context;
  private final E2eApiClient api;
  private final StoredImageRepository imageRepository;

  public CatalogSteps(
      E2eScenarioContext context, E2eApiClient api, StoredImageRepository imageRepository) {
    this.context = context;
    this.api = api;
    this.imageRepository = imageRepository;
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
    var imageId = createAvailableImage();
    var request =
        new CreateProductRequest(category.id(), imageId, productName, "Delicious pizza", price);

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

  private UUID createAvailableImage() {
    var checksum = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";
    var image =
        StoredImage.pending(
            context.tenantId(),
            "tenants/%s/images/%s.webp".formatted(context.tenantId(), UUID.randomUUID()),
            "image/webp",
            1_024L,
            checksum);
    image.confirm("image/webp", 1_024L, checksum);
    return imageRepository.save(image).getId();
  }
}
