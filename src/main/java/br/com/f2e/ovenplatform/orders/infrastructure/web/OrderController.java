package br.com.f2e.ovenplatform.orders.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.CreateOrderRequest;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PreAuthorize("hasAuthority('ORDER_CREATE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<OrderResponse> create(
      @CurrentTenantId UUID tenantId, @Valid @RequestBody CreateOrderRequest orderRequest) {

    var orderResponse =
        OrderResponse.from(orderService.createOrder(tenantId, orderRequest.toCommand()));
    var uri = ResourceUriBuilder.buildLocation(orderResponse.id());

    return ResponseEntity.created(uri).body(orderResponse);
  }

  @PreAuthorize("hasAuthority('ORDER_READ')")
  @GetMapping(version = API_VERSION_VALUE, path = "/{id}")
  public ResponseEntity<OrderResponse> findById(
      @CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    return orderService
        .findOrder(tenantId, id)
        .map(OrderResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PreAuthorize("hasAuthority('ORDER_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE, path = "/{id}/mark-ready")
  public ResponseEntity<Void> markAsReady(@CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    orderService.markAsReady(tenantId, id);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('ORDER_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE, path = "/{id}/complete")
  public ResponseEntity<Void> complete(@CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    orderService.complete(tenantId, id);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('ORDER_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE, path = "/{id}/cancel")
  public ResponseEntity<Void> cancel(@CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    orderService.cancel(tenantId, id);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('ORDER_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<OrderResponse>> list(@CurrentTenantId UUID tenantId) {
    var orders = orderService.listOrders(tenantId).stream().map(OrderResponse::from).toList();
    return ResponseEntity.ok(orders);
  }

  @PreAuthorize("hasAuthority('PAYMENT_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE, path = "/{orderId}/payment/mark-paid")
  public ResponseEntity<Void> markAsPaid(
      @CurrentTenantId UUID tenantId, @PathVariable UUID orderId) {
    orderService.markPaymentAsPaid(tenantId, orderId);
    return ResponseEntity.noContent().build();
  }
}
