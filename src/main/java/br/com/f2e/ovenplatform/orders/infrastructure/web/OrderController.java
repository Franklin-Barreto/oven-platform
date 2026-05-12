package br.com.f2e.ovenplatform.orders.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;

import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping(version = "1.0")
  ResponseEntity<OrderResponse> create(
      @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
      @Valid @RequestBody CreateOrderRequest orderRequest) {

    var orderResponse =
        OrderResponse.from(orderService.createOrder(tenantId, orderRequest.toCommand()));
    var uri = ResourceUriBuilder.buildLocation(orderResponse.id());

    return ResponseEntity.created(uri).body(orderResponse);
  }
}
