package br.com.f2e.ovenplatform.shared.infrastructure.tracing;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TraceContext {

  private final ThreadLocal<String> traceIdHolder = new ThreadLocal<>();

  public void setTraceId(String value) {
    this.traceIdHolder.set(value);
  }

  public String getTraceId() {
    if (traceIdHolder.get() == null) {
      throw new MissingTraceIdException();
    }
    return traceIdHolder.get();
  }

  public Optional<String> findTraceId() {
    return Optional.ofNullable(traceIdHolder.get());
  }

  public void clear() {
    traceIdHolder.remove();
  }
}
