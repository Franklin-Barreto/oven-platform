package br.com.f2e.ovenplatform.fulfillment.application;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.fulfillment.infrastructure.outbox.OutboxFulfillmentOrderReadyEventPublisher;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence.JpaOutboxEventRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;

@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({
  FulfillmentService.class,
  OutboxService.class,
  JpaOutboxEventRepository.class,
  OutboxFulfillmentOrderReadyEventPublisher.class
})
class FulfillmentServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Value("${oven.kafka.topics.fulfillment}")
  private String fulfillmentTopic;

  @Autowired private FulfillmentService fulfillmentService;
  @Autowired private OutboxEventRepository outboxEventRepository;

  @Test
  void shouldCreatePendingOutboxEventWhenPreparationIsReady() {
    fulfillmentService.handlePreparationReady(
        new PreparationReadyCommand(TENANT_ID, ORDER_ID, READY_AT));

    flushAndClear();

    var outboxEvent =
        outboxEventRepository
            .findByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, ORDER_ID, FULFILLMENT_ORDER_READY_EVENT)
            .orElseThrow();

    assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(outboxEvent.getTopic()).isEqualTo(fulfillmentTopic);
    assertThat(outboxEvent.getMessageKey()).isEqualTo(ORDER_ID.toString());
    assertThat(outboxEvent.getPayloadVersion()).isEqualTo(1);
    assertThat(outboxEvent.getAttempts()).isZero();
    assertThat(outboxEvent.getPublishedAt()).isNull();
    assertThat(outboxEvent.getLastError()).isNull();

    var payload = JsonUtils.fromJson(outboxEvent.getPayload(), FulfillmentOrderReadyPayload.class);

    assertThat(payload.tenantId()).isEqualTo(TENANT_ID);
    assertThat(payload.orderId()).isEqualTo(ORDER_ID);
    assertThat(payload.readyAt()).isEqualTo(READY_AT);
  }

  @Test
  void shouldNotCreateDuplicatedOutboxEventWhenPreparationReadyIsHandledAgain() {
    var repeatedReadyAt = Instant.parse("2026-05-12T20:45:00Z");

    fulfillmentService.handlePreparationReady(
        new PreparationReadyCommand(TENANT_ID, ORDER_ID, READY_AT));
    fulfillmentService.handlePreparationReady(
        new PreparationReadyCommand(TENANT_ID, ORDER_ID, repeatedReadyAt));

    flushAndClear();

    var outboxEventCount =
        entityManager
            .createQuery(
                """
                select count(event)
                from OutboxEvent event
                where event.aggregateType = :aggregateType
                  and event.aggregateId = :aggregateId
                  and event.eventType = :eventType
                """,
                Long.class)
            .setParameter("aggregateType", AGGREGATE_TYPE)
            .setParameter("aggregateId", ORDER_ID)
            .setParameter("eventType", FULFILLMENT_ORDER_READY_EVENT)
            .getSingleResult();

    assertThat(outboxEventCount).isOne();

    var outboxEvent =
        outboxEventRepository
            .findByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, ORDER_ID, FULFILLMENT_ORDER_READY_EVENT)
            .orElseThrow();

    assertThat(outboxEvent.getIdempotencyKey())
        .isEqualTo("%s:%s:%s".formatted(AGGREGATE_TYPE, ORDER_ID, FULFILLMENT_ORDER_READY_EVENT));
    var payload = JsonUtils.fromJson(outboxEvent.getPayload(), FulfillmentOrderReadyPayload.class);

    assertThat(payload.readyAt()).isEqualTo(READY_AT);
  }
}
