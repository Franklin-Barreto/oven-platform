package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String traceId,
    List<ApiErrorItem> errors,
    String path) {

  public static ApiErrorResponse of(
      HttpStatus status, String traceId, List<ApiErrorItem> errors, String path) {
    return new ApiErrorResponse(
        Instant.now(), status.value(), status.getReasonPhrase(), traceId, errors, path);
  }

  public static ApiErrorResponse of(
      HttpStatus status, String code, String traceId, String message, String path) {
    return new ApiErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        traceId,
        List.of(ApiErrorItem.messageOnly(code, message)),
        path);
  }
}
