package br.com.f2e.ovenplatform.kitchen.application;

import java.util.List;
import java.util.UUID;

public record CreateTicketCommand(
    UUID tenantId, UUID orderId, List<CreateTicketItemCommand> items) {

  public CreateTicketCommand {
    items = List.copyOf(items);
  }
}
