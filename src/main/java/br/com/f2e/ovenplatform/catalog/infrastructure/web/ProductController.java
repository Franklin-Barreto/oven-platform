package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;

import br.com.f2e.ovenplatform.catalog.application.CatalogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RequestMapping("/products")
@RestController
public class ProductController {

  private final CatalogService service;

  public ProductController(CatalogService service) {
    this.service = service;
  }

  @PostMapping(version = "1.0")
  public ResponseEntity<ProductResponse> create(
      @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
      @Valid @RequestBody CreateProductRequest productRequest,
      HttpServletRequest request) {
    var productResponse =
        ProductResponse.from(
            service.createProduct(tenantId, productRequest.name(), productRequest.price()));
    var uri =
        UriComponentsBuilder.fromPath(request.getRequestURI() + "/{id}")
            .buildAndExpand(productResponse.id())
            .toUri();
    return ResponseEntity.created(uri).body(productResponse);
  }

  @GetMapping(version = "1.0")
  public ResponseEntity<List<ProductResponse>> list(
      @RequestHeader(TENANT_ID_HEADER) UUID tenantId) {
    var products =
        service.listActiveProducts(tenantId).stream().map(ProductResponse::from).toList();

    return ResponseEntity.ok(products);
  }
}
