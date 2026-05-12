package br.com.f2e.ovenplatform.shared.infrastructure.web.test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultMatcher;

public final class ApiErrorResponseMatchers {

  private ApiErrorResponseMatchers() {}

  public static ResultMatcher[] expectValidationErrors(
      HttpStatus httpStatus,
      String path,
      String error,
      String code,
      String message,
      String field,
      int statusCode) {
    return new ResultMatcher[] {
      status().is(httpStatus.value()),
      jsonPath("$.path").value(path),
      jsonPath("$.error").value(error),
      jsonPath("$.traceId").isNotEmpty(),
      jsonPath("$.errors[0].code").value(code),
      jsonPath("$.errors[0].message").value(message),
      jsonPath("$.errors[0].field").value(field),
      jsonPath("$.status").value(statusCode)
    };
  }
}
