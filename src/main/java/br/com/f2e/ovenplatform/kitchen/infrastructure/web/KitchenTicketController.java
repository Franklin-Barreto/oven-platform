package br.com.f2e.ovenplatform.kitchen.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.infrastructure.web.dto.TicketResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kitchen/tickets")
public class KitchenTicketController {

  private final KitchenService service;

  public KitchenTicketController(KitchenService service) {
    this.service = service;
  }

  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<TicketResponse>> list(@CurrentTenantId UUID tenantId) {
    var response = service.list(tenantId).stream().map(TicketResponse::from).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping(version = API_VERSION_VALUE, path = "/{id}")
  public ResponseEntity<TicketResponse> findById(
      @CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    var response = TicketResponse.from(service.findByIdWithItems(tenantId, id));
    return ResponseEntity.ok(response);
  }

  @PostMapping(version = API_VERSION_VALUE, path = "/{id}/start-preparation")
  public ResponseEntity<Void> startPreparation(
      @CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    service.startPreparation(tenantId, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(version = API_VERSION_VALUE, path = "/{id}/mark-ready")
  public ResponseEntity<Void> markAsReady(@CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    service.markAsReady(tenantId, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(version = API_VERSION_VALUE, path = "/{id}/cancel")
  public ResponseEntity<Void> cancel(@CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    service.cancel(tenantId, id);
    return ResponseEntity.noContent().build();
  }
}
