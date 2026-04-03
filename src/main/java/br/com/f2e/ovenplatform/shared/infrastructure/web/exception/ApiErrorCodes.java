package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

public final class ApiErrorCodes {

  public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
  public static final String DUPLICATE_USER_EMAIL = "DUPLICATE_USER_EMAIL";
  public static final String TENANT_NOT_FOUND = "TENANT_NOT_FOUND";
  public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
  public static final String MISSING_REQUEST_HEADER = "MISSING_REQUEST_HEADER";
  public static final String INVALID_API_VERSION = "INVALID_API_VERSION";

  private ApiErrorCodes() {}
}
