package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.catalog.application.CatalogService;
import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/products")
@RestController
public class ProductController {

  private final CatalogService service;

  public ProductController(CatalogService service) {
    this.service = service;
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<ProductResponse> create(
      @CurrentTenantId UUID tenantId, @Valid @RequestBody CreateProductRequest productRequest) {

    var productResponse =
        ProductResponse.from(
            service.createProduct(
                tenantId,
                productRequest.categoryId(),
                productRequest.name(),
                productRequest.description(),
                productRequest.price()));
    var uri = ResourceUriBuilder.buildLocation(productResponse.id());
    return ResponseEntity.created(uri).body(productResponse);
  }

  @PreAuthorize("hasAuthority('CATALOG_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<ProductResponse>> list(@CurrentTenantId UUID tenantId) {
    var products =
        service.listActiveProducts(tenantId).stream().map(ProductResponse::from).toList();

    return ResponseEntity.ok(products);
  }

  @PreAuthorize("hasAuthority('CATALOG_READ')")
  @GetMapping(version = API_VERSION_VALUE, path = "/{id}")
  public ResponseEntity<ProductResponse> find(
      @CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    return ResponseEntity.ok(ProductResponse.from(service.getProduct(tenantId, id)));
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PatchMapping(version = API_VERSION_VALUE, path = "/{id}")
  public ResponseEntity<ProductResponse> update(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateProductRequest productRequest) {
    var product =
        service.update(
            tenantId,
            id,
            productRequest.categoryId(),
            productRequest.name(),
            productRequest.description(),
            productRequest.price(),
            productRequest.active());

    return ResponseEntity.ok(ProductResponse.from(product));
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @DeleteMapping(version = API_VERSION_VALUE, path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    service.deactivate(tenantId, id);
  }
}
