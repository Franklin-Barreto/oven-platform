package br.com.f2e.ovenplatform.shared.infrastructure.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceContextTest {

  TraceContext traceContext;

  @BeforeEach
  void setUp() {
    traceContext = new TraceContext();
  }

  @Test
  void shouldStoreAndReturnTraceId() {

    traceContext.setTraceId("trace-id-1777-ewe");
    assertEquals("trace-id-1777-ewe", traceContext.getTraceId());
  }

  @Test
  void shouldFailWhenTraceIdIsNotAvailable() {
    var exception = assertThrows(MissingTraceIdException.class, () -> traceContext.getTraceId());
    assertEquals(
        "Trace ID is not available in the current request context.", exception.getMessage());
  }

  @Test
  void shouldClearStoredTraceId() {
    traceContext.setTraceId("trace-id-1777-ewe");
    traceContext.clear();
    assertTrue(traceContext.findTraceId().isEmpty());
  }

  @Test
  void shouldIsolateTraceIdBetweenThreads() throws Exception {
    traceContext.setTraceId("trace-id-main-thread");
    assertEquals("trace-id-main-thread", traceContext.getTraceId());

    try (var executor = Executors.newSingleThreadExecutor()) {
      var future = executor.submit(traceContext::findTraceId);
      assertTrue(future.get().isEmpty());
    }
  }
}
