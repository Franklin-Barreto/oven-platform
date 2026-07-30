package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.catalog.application.variant.CreateProductVariantCommand;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantService;
import br.com.f2e.ovenplatform.catalog.application.variant.ReorderProductVariantsCommand;
import br.com.f2e.ovenplatform.catalog.application.variant.UpdateProductVariantCommand;
import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products/{productId}/variants")
public class ProductVariantController {

  private final ProductVariantService service;

  public ProductVariantController(ProductVariantService service) {
    this.service = service;
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<ProductVariantResponse> create(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @Valid @RequestBody CreateProductVariantRequest request) {

    var productVariant =
        service.create(
            tenantId,
            productId,
            new CreateProductVariantCommand(request.imageId(), request.name(), request.price()));

    var response = ProductVariantResponse.from(productVariant);
    var uri = ResourceUriBuilder.buildLocation(response.id());

    return ResponseEntity.created(uri).body(response);
  }

  @PreAuthorize("hasAuthority('CATALOG_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<ProductVariantResponse>> list(
      @CurrentTenantId UUID tenantId, @PathVariable UUID productId) {

    var responses =
        service.listVariants(tenantId, productId).stream()
            .map(ProductVariantResponse::from)
            .toList();

    return ResponseEntity.ok(responses);
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/{variantId}", version = API_VERSION_VALUE)
  public ResponseEntity<ProductVariantResponse> update(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID variantId,
      @RequestBody @Valid UpdateProductVariantRequest request) {

    var result =
        service.update(
            tenantId,
            productId,
            variantId,
            new UpdateProductVariantCommand(request.imageId(), request.name(), request.price()));

    return ResponseEntity.ok(ProductVariantResponse.from(result));
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/{variantId}/status", version = API_VERSION_VALUE)
  public ResponseEntity<Void> changeStatus(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID variantId,
      @RequestBody @Valid ChangeProductVariantStatusRequest request) {

    service.changeStatus(tenantId, productId, variantId, request.active());

    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/display-order", version = API_VERSION_VALUE)
  public ResponseEntity<Void> reorder(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @RequestBody @Valid ReorderProductVariantsRequest request) {

    service.reorder(tenantId, productId, new ReorderProductVariantsCommand(request.variantIds()));
    return ResponseEntity.noContent().build();
  }
}
