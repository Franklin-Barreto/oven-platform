package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @Mock private MessageSource messageSource;
  @Mock private Tracer tracer;
  @Mock private Span span;
  @Mock private io.micrometer.tracing.TraceContext traceContext;

  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    ApiErrorResponseFactory responseFactory = new ApiErrorResponseFactory(tracer);
    handler = new GlobalExceptionHandler(messageSource, responseFactory);
    request = new MockHttpServletRequest();
  }

  @Test
  void shouldIncludeCurrentTraceIdInErrorResponse() {
    doReturn(span).when(tracer).currentSpan();
    when(span.context()).thenReturn(traceContext);
    when(traceContext.traceId()).thenReturn("abc-123");

    var response =
        handler.illegalArgumentHandler(new IllegalArgumentException("exception"), request);

    assertNotNull(response.getBody());
    assertEquals("abc-123", response.getBody().traceId());
  }

  @Test
  void shouldReturnNullTraceIdWhenThereIsNoCurrentSpan() {
    var response =
        handler.illegalArgumentHandler(new IllegalArgumentException("exception"), request);

    assertNotNull(response.getBody());
    assertNull(response.getBody().traceId());
  }

  @Test
  void shouldBuildErrorResponseWithExpectedFields() {

    when(messageSource.getMessage(any(FieldError.class), any())).thenReturn("invalid field");
    doReturn(span).when(tracer).currentSpan();
    when(span.context()).thenReturn(traceContext);
    when(traceContext.traceId()).thenReturn("abc-123");

    var target = new Object();
    var objectName = "request";
    var bindingResult = new BeanPropertyBindingResult(target, objectName);

    bindingResult.addError(new FieldError(objectName, "email", "must not be blank"));
    bindingResult.addError(new FieldError(objectName, "name", "must not be null"));
    request.setRequestURI("/test");
    var exception =
        handler.badRequestHandler(
            new MethodArgumentNotValidException(null, bindingResult), request);
    var apiErrorResponse = exception.getBody();
    assertNotNull(apiErrorResponse);
    var errors = apiErrorResponse.errors();

    assertEquals(2, errors.size());

    assertEquals("email", errors.getFirst().field());
    assertEquals(VALIDATION_ERROR, errors.getFirst().code());
    assertEquals("invalid field", errors.getFirst().message());

    assertEquals("name", errors.getLast().field());
    assertEquals(VALIDATION_ERROR, errors.getLast().code());
    assertEquals("invalid field", errors.getLast().message());
    assertEquals(400, apiErrorResponse.status());
    assertNotNull(apiErrorResponse.traceId());
    assertEquals("/test", apiErrorResponse.path());
  }
}
