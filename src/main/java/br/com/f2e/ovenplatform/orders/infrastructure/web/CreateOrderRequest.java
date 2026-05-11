package br.com.f2e.ovenplatform.orders.infrastructure.web;

import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification =
        "Request DTO intentionally stores raw client input so Bean Validation can produce proper 400 responses for invalid/null collections.")
public record CreateOrderRequest(
    @NotNull(message = "must not be null")
        @Size(min = 1, message = "items must have at least 1 item")
        @Valid
        List<OrderItemRequest> items) {

  public CreateOrderCommand toCommand() {
    return new CreateOrderCommand(items.stream().map(OrderItemRequest::toCommand).toList());
  }
}
