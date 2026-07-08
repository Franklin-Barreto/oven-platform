package br.com.f2e.ovenplatform.kitchen.infrastructure.kafka;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.CreateTicketItemCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedKitchenTicketConsumer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(OrderCreatedKitchenTicketConsumer.class);

  private final KitchenService kitchenService;
  private final ObjectMapper mapper;

  public OrderCreatedKitchenTicketConsumer(KitchenService kitchenService, ObjectMapper mapper) {
    this.kitchenService = kitchenService;
    this.mapper = mapper;
  }

  @KafkaListener(
      topics = "${oven.kafka.topics.orders}",
      groupId = "${oven.kafka.consumer-groups.kitchen}")
  public void on(String payload) {
    var orderCreatedPayload = mapper.readValue(payload, OrderCreatedPayload.class);
    var command = toCommand(orderCreatedPayload);

    try {
      kitchenService.createTicketFromOrder(command);
    } catch (DataIntegrityViolationException _) {
      LOGGER.info(
          "Ignoring duplicated order.created event for tenantId={} orderId={}",
          command.tenantId(),
          command.orderId());
    }
  }

  private CreateTicketCommand toCommand(OrderCreatedPayload payload) {
    var commandItems =
        payload.items().stream()
            .map(
                item ->
                    new CreateTicketItemCommand(
                        item.productId(), item.productName(), item.quantity()))
            .toList();

    return new CreateTicketCommand(payload.tenantId(), payload.orderId(), commandItems);
  }
}
