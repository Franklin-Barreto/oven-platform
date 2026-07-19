package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseFactory {

  private final Tracer tracer;

  public ApiErrorResponseFactory(Tracer tracer) {
    this.tracer = tracer;
  }

  public ResponseEntity<ApiErrorResponse> create(
      HttpStatus status, List<ApiErrorItem> errors, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(status, resolveTraceId(), errors, request.getRequestURI()));
  }

  public ResponseEntity<ApiErrorResponse> create(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(
            ApiErrorResponse.of(status, code, resolveTraceId(), message, request.getRequestURI()));
  }

  private String resolveTraceId() {
    var currentSpan = tracer.currentSpan();
    return currentSpan != null ? currentSpan.context().traceId() : null;
  }
}
