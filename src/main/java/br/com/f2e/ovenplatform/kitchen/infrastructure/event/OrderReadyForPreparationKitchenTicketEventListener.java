package br.com.f2e.ovenplatform.kitchen.infrastructure.event;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.CreateTicketItemCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.orders.application.event.OrderReadyForPreparationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OrderReadyForPreparationKitchenTicketEventListener {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(OrderReadyForPreparationKitchenTicketEventListener.class);

  private final KitchenService kitchenService;

  public OrderReadyForPreparationKitchenTicketEventListener(KitchenService kitchenService) {
    this.kitchenService = kitchenService;
  }

  @ApplicationModuleListener(id = "kitchen-order-ready-for-preparation-listener")
  public void on(OrderReadyForPreparationEvent event) {
    var command = toCommand(event);

    try {
      kitchenService.createTicketFromOrder(command);
    } catch (DataIntegrityViolationException _) {
      LOGGER.info(
          "Ignoring duplicated order ready for preparation event for tenantId={} orderId={}",
          command.tenantId(),
          command.orderId());
    }
  }

  private CreateTicketCommand toCommand(OrderReadyForPreparationEvent event) {
    var commandItems =
        event.items().stream()
            .map(
                item ->
                    new CreateTicketItemCommand(
                        item.productId(),
                        item.productName(),
                        item.variantId(),
                        item.variantName(),
                        item.quantity()))
            .toList();

    return new CreateTicketCommand(event.tenantId(), event.orderId(), commandItems);
  }
}
