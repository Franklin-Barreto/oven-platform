package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
    Instant timestamp, int status, List<ApiErrorItem> errors, String path) {

  public static ApiErrorResponse of(HttpStatus status, List<ApiErrorItem> errors, String path) {
    return new ApiErrorResponse(Instant.now(), status.value(), errors, path);
  }

  public static ApiErrorResponse of(HttpStatus status, String message, String path) {
    return new ApiErrorResponse(
        Instant.now(), status.value(), List.of(ApiErrorItem.messageOnly(message)), path);
  }
}
