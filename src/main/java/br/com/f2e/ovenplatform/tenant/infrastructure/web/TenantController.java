package br.com.f2e.ovenplatform.tenant.infrastructure.web;

import br.com.f2e.ovenplatform.tenant.application.TenantService;
import br.com.f2e.ovenplatform.tenant.infrastructure.web.dto.CreateTenantRequest;
import br.com.f2e.ovenplatform.tenant.infrastructure.web.dto.TenantResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/tenants")
public class TenantController {

  private final TenantService tenantService;

  public TenantController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @PostMapping
  public ResponseEntity<TenantResponse> create(
      @Valid @RequestBody CreateTenantRequest request) {
    var response = TenantResponse.from(tenantService.create(request.name(), request.plan()));
    var uri = UriComponentsBuilder.fromPath("/tenants/{id}").buildAndExpand(response.id()).toUri();
    return ResponseEntity.created(uri).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<TenantResponse> findById(@PathVariable UUID id) {
    return tenantService
        .findById(id)
        .map(tenant -> ResponseEntity.ok(TenantResponse.from(tenant)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
