package br.com.f2e.ovenplatform.shared.infrastructure.tracing;

import static br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceConstants.TRACE_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTracingFilterTest {

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private TraceContext traceContext;
  private RequestTracingFilter filter;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    traceContext = new TraceContext();
    filter = new RequestTracingFilter(traceContext);

    request.setMethod(HttpMethod.GET.name());
    request.setRequestURI("/test");

    Logger logger = (Logger) LoggerFactory.getLogger(RequestTracingFilter.class);
    listAppender = new ListAppender<>();
    listAppender.start();

    logger.addAppender(listAppender);
  }

  @Test
  void shouldReuseTraceIdFromRequestHeader() throws Exception {

    var traceId = "abc-123";
    request.addHeader(ApiHeaders.TRACE_ID_HEADER, traceId);

    filter.doFilter(request, response, (_, _) -> assertTraceIdInContextAndMdc(traceId));

    assertEquals(traceId, response.getHeader(ApiHeaders.TRACE_ID_HEADER));
    assertTracingContextCleared();
    assertRequestCompletedLogContains("method=GET", "path=/test", "status=200", "durationMs=");
  }

  @Test
  void shouldGenerateTraceIdWhenHeaderIsMissing() throws Exception {
    var captured = new AtomicReference<String>();

    filter.doFilter(
        request,
        response,
        (_, _) -> {
          var traceId = assertGeneratedTraceIdInContextAndMdc();
          captured.set(traceId);
        });

    var headerTraceId = response.getHeader(ApiHeaders.TRACE_ID_HEADER);

    assertNotNull(headerTraceId);
    assertFalse(headerTraceId.isBlank());
    assertEquals(headerTraceId, captured.get());

    assertTracingContextCleared();
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
                  assertGeneratedTraceIdInContextAndMdc();
                  throw new ServletException("Exception thrown");
                }));

    assertTracingContextCleared();

    assertRequestCompletedLogContains("method=GET", "path=/test", "status=", "durationMs=");
  }

  private void assertTracingContextCleared() {
    assertTrue(traceContext.findTraceId().isEmpty());
    assertNull(MDC.get(TRACE_ID_KEY));
  }

  private String getSingleLogMessage() {
    assertFalse(listAppender.list.isEmpty());
    return listAppender.list.getFirst().getFormattedMessage();
  }

  private void assertRequestCompletedLogContains(String... expectedFragments) {
    var message = getSingleLogMessage();

    assertTrue(message.contains("request completed"));

    for (var fragment : expectedFragments) {
      assertTrue(message.contains(fragment));
    }
  }

  private void assertTraceIdInContextAndMdc(String expectedTraceId) {
    assertEquals(expectedTraceId, traceContext.getTraceId());
    assertEquals(expectedTraceId, MDC.get(TRACE_ID_KEY));
  }

  private String assertGeneratedTraceIdInContextAndMdc() {
    var traceId = traceContext.getTraceId();

    assertNotNull(traceId);
    assertFalse(traceId.isBlank());
    assertEquals(traceId, MDC.get(TRACE_ID_KEY));

    return traceId;
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(RequestTracingFilter.class);
    logger.detachAppender(listAppender);
  }
}
