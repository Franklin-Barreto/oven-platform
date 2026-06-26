package br.com.f2e.ovenplatform.identity.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_HEADER;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.application.CreateTenantUserCommand;
import br.com.f2e.ovenplatform.identity.application.IdentityService;
import br.com.f2e.ovenplatform.identity.application.TenantUserResult;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.UserRequest;
import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IdentityController.class)
@Import({TraceContext.class})
class IdentityControllerTest {

  private static final String EMAIL = "user.email@outlook.com";
  private static final String PASSWORD = "1234";
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID USER_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final String BASE_URL = "/users";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private IdentityService identityService;
  @MockitoBean private JwtService jwtService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreateUserSuccessfully() throws Exception {

    var request =
        new CreateTenantUserCommand(TENANT_ID, EMAIL, PASSWORD, TenantMembershipRole.MEMBER);
    when(identityService.createTenantUser(request)).thenReturn(tenantUserCreatedResponse());

    var userRequest = createUserRequest();

    var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(userRequest))
                    .with(authenticatedTenantUser(TENANT_ID))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(TenantMembershipStatus.ACTIVE.name()))
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + USER_ID);
  }

  @ParameterizedTest
  @MethodSource("invalidRequestsCreate")
  void shouldReturn400WhenCreateRequestIsInvalid(UserRequest request, String field, String message)
      throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .with(authenticatedTenantUser(TENANT_ID))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  @Test
  void shouldReturn200WhenUserExists() throws Exception {
    when(identityService.findTenantUserById(TENANT_ID, USER_ID))
        .thenReturn(tenantUserCreatedResponse());

    mockMvc
        .perform(
            get(BASE_URL + "/%s".formatted(USER_ID))
                .with(authenticatedTenantUser(TENANT_ID))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(TenantMembershipStatus.ACTIVE.name()))
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
  }

  @Test
  void shouldReturn404WhenUserDoesNotExist() throws Exception {
    var getUrl = BASE_URL + "/" + USER_ID;

    when(identityService.findTenantUserById(TENANT_ID, USER_ID))
        .thenThrow(new NoSuchElementException("User"));

    mockMvc
        .perform(
            get(getUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .with(authenticatedTenantUser(TENANT_ID))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.NOT_FOUND,
                getUrl,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ApiErrorCodes.RESOURCE_NOT_FOUND,
                "User",
                null,
                HttpStatus.NOT_FOUND.value()));
  }

  @Test
  void shouldReturn400WhenUserIdIsInvalid() throws Exception {
    var getUrl = BASE_URL + "/" + "invalidUserId";

    mockMvc
        .perform(
            get(getUrl).accept(MediaType.APPLICATION_JSON).with(authenticatedTenantUser(TENANT_ID)))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                getUrl,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_ARGUMENT,
                "Invalid UUID string: invalidUserId",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  @ParameterizedTest
  @MethodSource("dataIntegrityViolationScenarios")
  void shouldHandleDataIntegrityViolationDuringUserCreation(
      DataIntegrityViolationException exception,
      HttpStatus expectedStatus,
      String expectedCode,
      String expectedMessage)
      throws Exception {

    var request =
        new CreateTenantUserCommand(TENANT_ID, EMAIL, PASSWORD, TenantMembershipRole.MEMBER);
    when(identityService.createTenantUser(request)).thenThrow(exception);

    var userRequest = createUserRequest();

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(userRequest))
                .with(authenticatedTenantUser(TENANT_ID))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                expectedStatus,
                BASE_URL,
                expectedStatus.getReasonPhrase(),
                expectedCode,
                expectedMessage,
                null,
                expectedStatus.value()));
  }

  @Test
  void shouldReturn400ForUnsupportedApiVersion() throws Exception {
    var getUrl = BASE_URL + "/" + USER_ID;

    mockMvc
        .perform(
            get(getUrl)
                .accept(MediaType.APPLICATION_JSON)
                .with(authenticatedTenantUser(TENANT_ID))
                .header(API_VERSION_HEADER, "3.0.0"))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                getUrl,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_API_VERSION,
                "400 BAD_REQUEST \"Invalid API version: '3.0.0'.\"",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  @Test
  void shouldReturnBadRequestWhenApiVersionIsSupportedButNotAcceptedByEndpoint() throws Exception {
    var getUrl = BASE_URL + "/" + USER_ID;

    mockMvc
        .perform(
            get(getUrl)
                .accept(MediaType.APPLICATION_JSON)
                .with(authenticatedTenantUser(TENANT_ID))
                .header(API_VERSION_HEADER, "2.0.0"))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                getUrl,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_API_VERSION,
                "400 BAD_REQUEST \"Invalid API version: '2.0.0'.\"",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  private static Stream<Arguments> invalidRequestsCreate() {
    return Stream.of(
        Arguments.of(
            new UserRequest("invalidEmail.com", "1234", TenantMembershipRole.MEMBER),
            "email",
            "must be a well-formed email address"),
        Arguments.of(
            new UserRequest("user@email.com", "", TenantMembershipRole.MEMBER),
            "password",
            "must not be blank"),
        Arguments.of(new UserRequest("user@email.com", "1234", null), "role", "must not be null"));
  }

  private static Stream<Arguments> dataIntegrityViolationScenarios() {
    return Stream.of(
        Arguments.of(
            directConstraintViolation("uk_users_tenant_id_email"),
            HttpStatus.CONFLICT,
            ApiErrorCodes.DUPLICATE_USER_EMAIL,
            "A user with this email already exists."),
        Arguments.of(
            nestedConstraintViolation("fk_users_tenant_id"),
            HttpStatus.NOT_FOUND,
            ApiErrorCodes.TENANT_NOT_FOUND,
            "Tenant not found."),
        Arguments.of(
            withoutConstraintViolation(),
            HttpStatus.CONFLICT,
            ApiErrorCodes.DATA_INTEGRITY_VIOLATION,
            "Data integrity violation."));
  }

  private static UserRequest createUserRequest() {
    return new UserRequest(EMAIL, PASSWORD, TenantMembershipRole.MEMBER);
  }

  private static TenantUserResult tenantUserCreatedResponse() {
    return new TenantUserResult(
        USER_ID, TENANT_ID, EMAIL, TenantMembershipRole.MEMBER, TenantMembershipStatus.ACTIVE);
  }

  private static DataIntegrityViolationException directConstraintViolation(String constraintName) {
    return new DataIntegrityViolationException(
        "data integrity violation",
        new ConstraintViolationException("constraint violation", null, constraintName));
  }

  private static DataIntegrityViolationException nestedConstraintViolation(String constraintName) {
    var nested = new ConstraintViolationException("constraint violation", null, constraintName);

    return new DataIntegrityViolationException(
        "data integrity violation", new RuntimeException("wrapper", nested));
  }

  private static DataIntegrityViolationException withoutConstraintViolation() {
    return new DataIntegrityViolationException(
        "data integrity violation", new RuntimeException("wrapper without constraint"));
  }
}
