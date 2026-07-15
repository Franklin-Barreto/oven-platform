package br.com.f2e.ovenplatform.shared.application.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  private static final Instant NOW = Instant.parse("2026-06-28T20:00:00Z");
  private static final String AGGREGATE_TYPE = "TEST_AGGREGATE";
  private static final String EVENT_TYPE = "test.event";
  private static final String TEST_TOPIC = "test-events";

  @Mock private OutboxEventRepository repository;
  @Mock private OutboxEventPublisher eventPublisher;

  private OutboxPublisher outboxPublisher;

  @BeforeEach
  void setUp() {
    outboxPublisher =
        new OutboxPublisher(repository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void shouldPublishPendingEventsAndMarkThemAsPublished() {
    var event = pendingEvent();
    var events = List.of(event);
    when(repository.findPendingEvents(100)).thenReturn(events);

    outboxPublisher.publishPendingEvents();

    verify(eventPublisher).publish(event);
    verify(repository).saveAll(events);

    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    assertThat(event.getPublishedAt()).isEqualTo(NOW);
    assertThat(event.getLastError()).isNull();
  }

  @Test
  void shouldMarkEventAsFailedWhenPublishFails() {
    var event = pendingEvent();
    var events = List.of(event);
    when(repository.findPendingEvents(100)).thenReturn(events);
    doThrow(new IllegalStateException("Kafka unavailable")).when(eventPublisher).publish(event);

    outboxPublisher.publishPendingEvents();

    verify(eventPublisher).publish(event);
    verify(repository).saveAll(events);

    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    assertThat(event.getAttempts()).isEqualTo(1);
    assertThat(event.getLastError()).isEqualTo("Kafka unavailable");
    assertThat(event.getPublishedAt()).isNull();
  }

  @Test
  void shouldDoNothingWhenThereAreNoPendingEvents() {
    when(repository.findPendingEvents(100)).thenReturn(List.of());

    outboxPublisher.publishPendingEvents();

    verify(repository).findPendingEvents(100);
    verifyNoMoreInteractions(repository);
    verifyNoMoreInteractions(eventPublisher);
  }

  private OutboxEvent pendingEvent() {
    var orderId = UUID.randomUUID();
    return OutboxEvent.pending(
        AGGREGATE_TYPE,
        orderId,
        EVENT_TYPE,
        TEST_TOPIC,
        orderId.toString(),
        "{\"orderId\":\"%s\"}".formatted(orderId),
        1);
  }
}
