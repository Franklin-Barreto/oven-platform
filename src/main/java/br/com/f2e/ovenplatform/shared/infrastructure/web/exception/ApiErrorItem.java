package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

public record ApiErrorItem(String field, String message) {

  public static ApiErrorItem of(String field, String message) {
    return new ApiErrorItem(field, message);
  }

  public static ApiErrorItem messageOnly(String message) {
    return new ApiErrorItem(null, message);
  }
}
