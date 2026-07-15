package br.com.f2e.ovenplatform.fulfillment.application;

import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Mock private ApplicationEventPublisher eventPublisher;

  private FulfillmentService fulfillmentService;

  @BeforeEach
  void setUp() {
    fulfillmentService = new FulfillmentService(eventPublisher);
  }

  @Test
  void shouldPublishCanonicalEventWhenPreparationIsReady() {
    fulfillmentService.handlePreparationReady(
        new PreparationReadyCommand(TENANT_ID, ORDER_ID, READY_AT));

    verify(eventPublisher)
        .publishEvent(new FulfillmentOrderMarkedAsReadyEvent(TENANT_ID, ORDER_ID, READY_AT));
  }
}
