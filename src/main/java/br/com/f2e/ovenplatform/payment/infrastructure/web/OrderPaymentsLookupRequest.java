package br.com.f2e.ovenplatform.payment.infrastructure.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record OrderPaymentsLookupRequest(
    @NotEmpty(message = "At least one order id must be provided") List<UUID> orderIds) {

  public OrderPaymentsLookupRequest {
    if (orderIds != null) {
      orderIds = List.copyOf(orderIds);
    }
  }

  @Override
  public List<UUID> orderIds() {
    return List.copyOf(orderIds);
  }
}
