package br.com.f2e.ovenplatform.orders.infrastructure.kafka;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.TOPIC;

import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.infrastructure.kafka.payload.FulfillmentOrderReadyPayload;
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

  @KafkaListener(topics = TOPIC, groupId = "oven-platform-orders")
  public void on(String payload) {
    var orderReadyPayload = mapper.readValue(payload, FulfillmentOrderReadyPayload.class);
    orderService.markAsReady(
        orderReadyPayload.tenantId(), orderReadyPayload.orderId(), orderReadyPayload.readyAt());
  }
}
