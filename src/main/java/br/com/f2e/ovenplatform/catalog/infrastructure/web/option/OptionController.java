package br.com.f2e.ovenplatform.catalog.infrastructure.web.option;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.catalog.application.option.CreateOptionCommand;
import br.com.f2e.ovenplatform.catalog.application.option.OptionService;
import br.com.f2e.ovenplatform.catalog.application.option.ReorderOptionsCommand;
import br.com.f2e.ovenplatform.catalog.application.option.UpdateOptionCommand;
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
@RequestMapping("/products/{productId}/option-groups/{optionGroupId}/options")
public class OptionController {

  private final OptionService service;

  public OptionController(OptionService service) {
    this.service = service;
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<OptionResponse> create(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID optionGroupId,
      @Valid @RequestBody CreateOptionRequest request) {
    OptionResponse response =
        OptionResponse.from(
            service.create(
                tenantId,
                productId,
                optionGroupId,
                new CreateOptionCommand(request.name(), request.priceAdjustment())));

    return ResponseEntity.created(ResourceUriBuilder.buildLocation(response.id())).body(response);
  }

  @PreAuthorize("hasAuthority('CATALOG_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<OptionResponse>> list(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID optionGroupId) {
    return ResponseEntity.ok(
        service.list(tenantId, productId, optionGroupId).stream()
            .map(OptionResponse::from)
            .toList());
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/{optionId}", version = API_VERSION_VALUE)
  public ResponseEntity<OptionResponse> update(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID optionGroupId,
      @PathVariable UUID optionId,
      @Valid @RequestBody UpdateOptionRequest request) {
    return ResponseEntity.ok(
        OptionResponse.from(
            service.update(
                tenantId,
                productId,
                optionGroupId,
                optionId,
                new UpdateOptionCommand(request.name(), request.priceAdjustment()))));
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/{optionId}/status", version = API_VERSION_VALUE)
  public ResponseEntity<Void> changeStatus(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID optionGroupId,
      @PathVariable UUID optionId,
      @Valid @RequestBody ChangeOptionStatusRequest request) {
    service.changeStatus(tenantId, productId, optionGroupId, optionId, request.active());
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/display-order", version = API_VERSION_VALUE)
  public ResponseEntity<Void> reorder(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID optionGroupId,
      @Valid @RequestBody ReorderOptionsRequest request) {
    service.reorder(
        tenantId, productId, optionGroupId, new ReorderOptionsCommand(request.optionIds()));
    return ResponseEntity.noContent().build();
  }
}
