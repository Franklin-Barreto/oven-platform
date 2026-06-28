package br.com.f2e.ovenplatform.shared.domain.outbox;

public enum OutboxEventStatus {
  PENDING,
  PUBLISHED,
  FAILED
}
