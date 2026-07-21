package br.com.f2e.ovenplatform.identity.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.application.AuthService;
import br.com.f2e.ovenplatform.identity.application.TenantMembershipAuthenticationService;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantMembershipInactiveException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginRequest;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(AuthenticationController.class)
@Import({ApiErrorResponseFactory.class})
class AuthenticationControllerTest {

  private static final String URL = "/auth/login";
  private static final UUID TENANT_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private TenantMembershipAuthenticationService membershipAuthenticationService;
  @MockitoBean private Tracer tracer;
  @MockitoBean private Span span;
  @MockitoBean private TraceContext traceContext;

  @BeforeEach
  void setUp() {
    doReturn(span).when(tracer).currentSpan();
    when(span.context()).thenReturn(traceContext);
    when(traceContext.traceId()).thenReturn("abc-123");
  }

  @Test
  void shouldLoginWithTenantIdAndReturnJwtToken() throws Exception {
    when(authService.login(TENANT_ID, "john@email.com", "123456")).thenReturn("jwt-token");

    var request = createLoginRequest();

    mockMvc
        .perform(
            post(URL).contentType(MediaType.APPLICATION_JSON).content(JsonUtils.toJson(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"));

    verify(authService).login(TENANT_ID, "john@email.com", "123456");
  }

  @Test
  void shouldReturnBadRequestWhenLoginRequestIsInvalid() throws Exception {
    var request = new LoginRequest(null, "", "");

    mockMvc
        .perform(
            post(URL).contentType(MediaType.APPLICATION_JSON).content(JsonUtils.toJson(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(authService);
  }

  @ParameterizedTest
  @MethodSource("tenantAccessFailureScenarios")
  void shouldMapTenantAccessFailureToExpectedErrorResponse(
      ResultMatcher expectedStatus,
      HttpStatus expectedHttpStatus,
      String expectedErrorCode,
      String expectedMessage,
      RuntimeException exception)
      throws Exception {
    when(authService.login(TENANT_ID, "john@email.com", "123456")).thenThrow(exception);

    var request = createLoginRequest();

    mockMvc
        .perform(
            post(URL).contentType(MediaType.APPLICATION_JSON).content(JsonUtils.toJson(request)))
        .andExpect(expectedStatus)
        .andExpectAll(
            expectValidationErrors(
                expectedHttpStatus,
                URL,
                expectedHttpStatus.getReasonPhrase(),
                expectedErrorCode,
                expectedMessage,
                null,
                expectedHttpStatus.value()));

    verify(authService).login(TENANT_ID, "john@email.com", "123456");
  }

  private static Stream<Arguments> tenantAccessFailureScenarios() {
    return Stream.of(
        Arguments.of(
            status().isForbidden(),
            HttpStatus.FORBIDDEN,
            ApiErrorCodes.INACTIVE_TENANT_MEMBERSHIP,
            "Tenant membership is inactive.",
            new TenantMembershipInactiveException()),
        Arguments.of(
            status().isForbidden(),
            HttpStatus.FORBIDDEN,
            ApiErrorCodes.TENANT_ACCESS_DENIED,
            "User does not have access to this tenant.",
            new TenantAccessDeniedException()));
  }

  private static LoginRequest createLoginRequest() {
    return new LoginRequest(TENANT_ID, "john@email.com", "123456");
  }
}
