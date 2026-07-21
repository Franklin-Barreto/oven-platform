package br.com.f2e.ovenplatform.e2e.context;

import br.com.f2e.ovenplatform.catalog.infrastructure.web.CategoryResponse;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.ProductResponse;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderResponse;
import io.cucumber.spring.ScenarioScope;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class E2eScenarioContext {

  private UUID tenantId;
  private String userEmail;
  private String userPassword;
  private String accessToken;
  private int lastResponseStatus;
  private OrderResponse lastOrderResponse;

  private final Map<String, CategoryResponse> categoriesByName = new HashMap<>();
  private final Map<String, ProductResponse> productsByName = new HashMap<>();

  public UUID tenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public String userEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  public String userPassword() {
    return userPassword;
  }

  public void setUserPassword(String userPassword) {
    this.userPassword = userPassword;
  }

  public String accessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public int lastResponseStatus() {
    return lastResponseStatus;
  }

  public void setLastResponseStatus(int lastResponseStatus) {
    this.lastResponseStatus = lastResponseStatus;
  }

  public void addCategory(CategoryResponse category) {
    categoriesByName.put(category.name(), category);
  }

  public CategoryResponse categoryNamed(String name) {
    return categoriesByName.get(name);
  }

  public void addProduct(ProductResponse product) {
    productsByName.put(product.name(), product);
  }

  public ProductResponse productNamed(String name) {
    return productsByName.get(name);
  }

  public OrderResponse lastOrderResponse() {
    return lastOrderResponse;
  }

  public void setLastOrderResponse(OrderResponse lastOrderResponse) {
    this.lastOrderResponse = lastOrderResponse;
  }
}
