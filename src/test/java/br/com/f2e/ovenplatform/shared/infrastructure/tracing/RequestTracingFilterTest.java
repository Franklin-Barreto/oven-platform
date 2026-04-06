package br.com.f2e.ovenplatform.shared.infrastructure.tracing;

import static br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceConstants.TRACE_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTracingFilterTest {

  private TraceContext traceContext;
  private RequestTracingFilter filter;
  private final MockHttpServletRequest request =  new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @BeforeEach
  void setUp() {
    traceContext = new TraceContext();
    filter = new RequestTracingFilter(traceContext);
  }

  @Test
  void shouldReuseTraceIdFromRequestHeader() throws Exception {

    var traceId = "abc-123";
    request.addHeader(ApiHeaders.TRACE_ID_HEADER, traceId);

    filter.doFilter(
        request,
        response,
        (_, _) -> {
          assertEquals(traceId, traceContext.getTraceId());
          assertEquals(traceId, MDC.get(TRACE_ID_KEY));
        });

    assertEquals(traceId, response.getHeader(ApiHeaders.TRACE_ID_HEADER));
    assertTrue(traceContext.findTraceId().isEmpty());
    assertNull(MDC.get(TRACE_ID_KEY));
  }

  @Test
  void shouldGenerateTraceIdWhenHeaderIsMissing() throws ServletException, IOException {
    var capturedTraceId = new AtomicReference<String>();
    filter.doFilter(
        request,
        response,
        (_, _) -> {
          var traceId = traceContext.getTraceId();
          capturedTraceId.set(traceId);

          assertNotNull(traceId);
          assertFalse(traceId.isBlank());
          assertEquals(traceId, MDC.get(TRACE_ID_KEY));
        });

    var headerTraceId = response.getHeader(ApiHeaders.TRACE_ID_HEADER);

    assertNotNull(headerTraceId);
    assertFalse(headerTraceId.isBlank());
    assertEquals(headerTraceId, capturedTraceId.get());
    assertTrue(traceContext.findTraceId().isEmpty());
    assertNull(MDC.get(TRACE_ID_KEY));
  }

  @Test
  void shouldClearTraceContextAndMdcWhenFilterChainThrowsException() {

    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                request,
                response,
                (_, _) -> {
                  var traceId = traceContext.getTraceId();

                  assertNotNull(traceId);
                  assertFalse(traceId.isBlank());
                  assertEquals(traceId, MDC.get(TRACE_ID_KEY));
                  throw new ServletException("Exception thrown");
                }));

    var headerTraceId = response.getHeader(ApiHeaders.TRACE_ID_HEADER);

    assertNotNull(headerTraceId);
    assertFalse(headerTraceId.isBlank());
    assertTrue(traceContext.findTraceId().isEmpty());
    assertNull(MDC.get(TRACE_ID_KEY));
  }
}
