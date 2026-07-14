package br.com.f2e.ovenplatform.kitchen.infrastructure.event;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.CreateTicketItemCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedKitchenTicketEventListener {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(OrderCreatedKitchenTicketEventListener.class);

  private final KitchenService kitchenService;

  public OrderCreatedKitchenTicketEventListener(KitchenService kitchenService) {
    this.kitchenService = kitchenService;
  }

  @ApplicationModuleListener(id = "kitchen-order-created-listener")
  public void on(OrderCreatedEvent event) {
    var command = toCommand(event);

    try {
      kitchenService.createTicketFromOrder(command);
    } catch (DataIntegrityViolationException _) {
      LOGGER.info(
          "Ignoring duplicated order.created event for tenantId={} orderId={}",
          command.tenantId(),
          command.orderId());
    }
  }

  private CreateTicketCommand toCommand(OrderCreatedEvent event) {
    var commandItems =
        event.items().stream()
            .map(
                item ->
                    new CreateTicketItemCommand(
                        item.productId(), item.productName(), item.quantity()))
            .toList();

    return new CreateTicketCommand(event.tenantId(), event.orderId(), commandItems);
  }
}
