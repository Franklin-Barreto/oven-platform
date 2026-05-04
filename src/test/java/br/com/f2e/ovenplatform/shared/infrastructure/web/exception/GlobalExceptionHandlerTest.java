package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
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

  private TraceContext traceContext;
  private GlobalExceptionHandler handler;
  @Mock private MessageSource messageSource;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    traceContext = new TraceContext();
    handler = new GlobalExceptionHandler(messageSource, traceContext);
    request = new MockHttpServletRequest();
  }

  @Test
  void shouldIncludeTraceIdFromContextInErrorResponse() {
    traceContext.setTraceId("abc-123");
    var exception =
        handler.illegalArgumentHandler(new IllegalArgumentException("exception"), request);
    var apiErrorResponse = exception.getBody();
    assertNotNull(apiErrorResponse);
    assertEquals("abc-123", apiErrorResponse.traceId());
  }

  @Test
  void shouldGenerateFallbackTraceIdWhenContextIsEmpty() {
    var exception =
        handler.illegalArgumentHandler(new IllegalArgumentException("exception"), request);
    var apiErrorResponse = exception.getBody();
    assertNotNull(apiErrorResponse);
    assertFalse(apiErrorResponse.traceId().isBlank());
    assertNotNull(apiErrorResponse.traceId());
  }

  @Test
  void shouldBuildErrorResponseWithExpectedFields() {

    when(messageSource.getMessage(any(FieldError.class), any())).thenReturn("invalid field");

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
