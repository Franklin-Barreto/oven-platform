package br.com.f2e.ovenplatform.shared.domain.outbox;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "outbox_events")
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(length = 100, nullable = false)
  private String aggregateType;

  @Column(nullable = false)
  private UUID aggregateId;

  @Column(length = 150, nullable = false)
  private String eventType;

  @Column(length = 150, nullable = false)
  private String topic;

  @Column(length = 150, nullable = false)
  private String messageKey;

  @Column(nullable = false)
  private String payload;

  @Column(nullable = false)
  private int payloadVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OutboxEventStatus status;

  @Column(nullable = false)
  private int attempts;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  private Instant publishedAt;

  private String lastError;

  @SuppressWarnings("unused")
  protected OutboxEvent() {}

  private OutboxEvent(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String topic,
      String messageKey,
      String payload,
      int payloadVersion) {
    this.aggregateType = requireNotBlank(aggregateType, "aggregateType");
    this.aggregateId = requireNotNull(aggregateId, "aggregateId");
    this.eventType = requireNotBlank(eventType, "eventType");
    this.topic = requireNotBlank(topic, "topic");
    this.messageKey = requireNotBlank(messageKey, "messageKey");
    this.payload = requireNotBlank(payload, "payload");
    this.payloadVersion = requirePositive(payloadVersion, "payloadVersion");
    this.status = OutboxEventStatus.PENDING;
    this.attempts = 0;
  }

  public static OutboxEvent pending(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String topic,
      String messageKey,
      String payload,
      int payloadVersion) {
    return new OutboxEvent(
        aggregateType, aggregateId, eventType, topic, messageKey, payload, payloadVersion);
  }

  public UUID getId() {
    return id;
  }

  public String getTopic() {
    return topic;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public String getPayload() {
    return payload;
  }

  public int getPayloadVersion() {
    return payloadVersion;
  }

  public OutboxEventStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void markAsPublished(Instant publishedAt) {
    this.status = OutboxEventStatus.PUBLISHED;
    this.publishedAt = publishedAt;
    this.lastError = null;
  }

  public void markAsFailed(String error) {
    this.status = OutboxEventStatus.FAILED;
    this.attempts++;
    this.lastError = error;
  }
}
