package br.com.f2e.ovenplatform.shared.infrastructure.web;

public final class ApiHeaders {

  public static final String TRACE_ID_HEADER = "X-Trace-Id";
  public static final String TENANT_ID_HEADER = "X-Tenant-Id";
  public static final String API_VERSION_HEADER = "X-API-Version";
  public static final String API_VERSION_VALUE = "1.0";
  public static final String AUTHORIZATON_HEADER = "Authorization";

  private ApiHeaders() {}
}
