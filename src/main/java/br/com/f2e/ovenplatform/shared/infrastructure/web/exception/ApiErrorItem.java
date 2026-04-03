package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

public record ApiErrorItem(String code, String field, String message) {

  public static ApiErrorItem of(String code, String field, String message) {
    return new ApiErrorItem(code, field, message);
  }

  public static ApiErrorItem messageOnly(String code, String message) {
    return new ApiErrorItem(code, null, message);
  }
}
