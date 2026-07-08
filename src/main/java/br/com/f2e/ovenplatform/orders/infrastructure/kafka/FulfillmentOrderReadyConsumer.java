package br.com.f2e.ovenplatform.orders.infrastructure.kafka;

import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class FulfillmentOrderReadyConsumer {

  private final OrderService orderService;
  private final ObjectMapper mapper;

  public FulfillmentOrderReadyConsumer(OrderService orderService, ObjectMapper mapper) {
    this.orderService = orderService;
    this.mapper = mapper;
  }

  @KafkaListener(
      topics = "${oven.kafka.topics.fulfillment}",
      groupId = "${oven.kafka.consumer-groups.orders}")
  public void on(String payload) {
    var orderReadyPayload = mapper.readValue(payload, FulfillmentOrderReadyPayload.class);
    orderService.markAsReady(
        orderReadyPayload.tenantId(), orderReadyPayload.orderId(), orderReadyPayload.readyAt());
  }
}
