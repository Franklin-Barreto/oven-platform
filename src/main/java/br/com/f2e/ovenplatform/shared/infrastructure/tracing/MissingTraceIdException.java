package br.com.f2e.ovenplatform.shared.infrastructure.tracing;

public class MissingTraceIdException extends RuntimeException {

  public MissingTraceIdException() {
    super("Trace ID is not available in the current request context.");
  }
}
