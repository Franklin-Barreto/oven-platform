package br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.*;
import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products/{productId}/option-groups")
public class OptionGroupController {

  private final OptionGroupService service;

  public OptionGroupController(OptionGroupService service) {
    this.service = service;
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<OptionGroupResponse> create(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @Valid @RequestBody CreateOptionGroupRequest request) {
    OptionGroupResult result =
        service.create(
            tenantId,
            productId,
            new CreateOptionGroupCommand(
                request.name(), request.minimumSelections(), request.maximumSelections()));
    OptionGroupResponse response = OptionGroupResponse.from(result);

    return ResponseEntity.created(ResourceUriBuilder.buildLocation(response.id())).body(response);
  }

  @PreAuthorize("hasAuthority('CATALOG_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<OptionGroupResponse>> list(
      @CurrentTenantId UUID tenantId, @PathVariable UUID productId) {
    return ResponseEntity.ok(
        service.list(tenantId, productId).stream().map(OptionGroupResponse::from).toList());
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/{optionGroupId}", version = API_VERSION_VALUE)
  public ResponseEntity<OptionGroupResponse> update(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID optionGroupId,
      @Valid @RequestBody UpdateOptionGroupRequest request) {
    OptionGroupResult result =
        service.update(
            tenantId,
            productId,
            optionGroupId,
            new UpdateOptionGroupCommand(
                request.name(), request.minimumSelections(), request.maximumSelections()));

    return ResponseEntity.ok(OptionGroupResponse.from(result));
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/{optionGroupId}/status", version = API_VERSION_VALUE)
  public ResponseEntity<Void> changeStatus(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @PathVariable UUID optionGroupId,
      @Valid @RequestBody ChangeOptionGroupStatusRequest request) {
    service.changeStatus(tenantId, productId, optionGroupId, request.active());
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/display-order", version = API_VERSION_VALUE)
  public ResponseEntity<Void> reorder(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID productId,
      @Valid @RequestBody ReorderOptionGroupsRequest request) {
    service.reorder(tenantId, productId, new ReorderOptionGroupsCommand(request.optionGroupIds()));
    return ResponseEntity.noContent().build();
  }
}
