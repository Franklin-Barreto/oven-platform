package br.com.f2e.ovenplatform.fulfillment.infrastructure.kafka;

import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.TOPIC;

import br.com.f2e.ovenplatform.fulfillment.application.FulfillmentService;
import br.com.f2e.ovenplatform.fulfillment.application.PreparationReadyCommand;
import br.com.f2e.ovenplatform.fulfillment.infrastructure.kafka.payload.KitchenTicketReadyPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class KitchenTicketReadyConsumer {

  private final FulfillmentService fulfillmentService;
  private final ObjectMapper mapper;

  public KitchenTicketReadyConsumer(FulfillmentService fulfillmentService, ObjectMapper mapper) {
    this.fulfillmentService = fulfillmentService;
    this.mapper = mapper;
  }

  @KafkaListener(topics = TOPIC, groupId = "oven-platform-fulfillment")
  public void on(String payload) {
    var ticketReadyPayload = mapper.readValue(payload, KitchenTicketReadyPayload.class);
    fulfillmentService.handlePreparationReady(
        new PreparationReadyCommand(
            ticketReadyPayload.tenantId(),
            ticketReadyPayload.orderId(),
            ticketReadyPayload.readyAt()));
  }
}
