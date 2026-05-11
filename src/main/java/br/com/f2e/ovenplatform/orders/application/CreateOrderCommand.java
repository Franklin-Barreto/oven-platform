package br.com.f2e.ovenplatform.orders.application;

import java.util.List;

public record CreateOrderCommand(List<CreateOrderItemCommand> items) {

  public CreateOrderCommand {
    items = List.copyOf(items);
  }
}
