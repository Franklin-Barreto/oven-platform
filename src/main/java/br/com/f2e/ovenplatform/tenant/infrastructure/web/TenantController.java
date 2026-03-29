package br.com.f2e.ovenplatform.tenant.infrastructure.web;

import br.com.f2e.ovenplatform.tenant.application.TenantService;
import br.com.f2e.ovenplatform.tenant.infrastructure.web.dto.CreateTenantRequest;
import br.com.f2e.ovenplatform.tenant.infrastructure.web.dto.CreateTenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<CreateTenantResponse> create(
      @Valid @RequestBody CreateTenantRequest request) {
    var response = CreateTenantResponse.from(tenantService.create(request.name(), request.plan()));
    var uri = UriComponentsBuilder.fromPath("/tenants/{id}").buildAndExpand(response.id()).toUri();
    return ResponseEntity.created(uri).body(response);
  }
}
