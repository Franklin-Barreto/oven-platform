package br.com.f2e.ovenplatform.shared.application.outbox;

public class OutboxPayloadSerializationException extends RuntimeException {

  public OutboxPayloadSerializationException(Throwable cause) {
    super("Could not serialize outbox event payload", cause);
  }
}
