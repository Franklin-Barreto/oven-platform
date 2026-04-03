package br.com.f2e.ovenplatform.identity.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.application.IdentityService;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import br.com.f2e.ovenplatform.identity.domain.UserStatus;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.UserRequest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(IdentityController.class)
class IdentityControllerTest {

  private static final String EMAIL = "user.email@outlook.com";
  private static final String PASSWORD = "1234";
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID USER_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final String URL = "/users";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private IdentityService identityService;

  @Test
  void shouldCreateUserSuccessfully() throws Exception {
    when(identityService.create(TENANT_ID, EMAIL, PASSWORD, UserRole.MEMBER))
        .thenReturn(getUserEntity());

    var userRequest = createUserRequest();

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(userRequest))
                .header("X-Tenant-Id", TENANT_ID)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString(URL)))
        .andExpect(jsonPath("$.status").value(UserStatus.ACTIVE.name()))
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
  }

  @ParameterizedTest
  @MethodSource("invalidRequestsCreate")
  void shouldReturn400WhenCreateRequestIsInvalid(UserRequest request, String field, String message)
      throws Exception {
    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .header("X-Tenant-Id", TENANT_ID)
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            validationErrors(
                HttpStatus.BAD_REQUEST,
                URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  @Test
  void shouldReturn400WhenCreateTenantIdHeaderIsInvalid() throws Exception {
    var userRequest = createUserRequest();

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(userRequest))
                .header("X-Tenant-Id", "invalid-uuid"))
        .andExpectAll(
            validationErrors(
                HttpStatus.BAD_REQUEST,
                URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Invalid UUID string: invalid-uuid",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  @Test
  void shouldReturn400WhenCreateTenantIdHeaderIsMissing() throws Exception {
    var userRequest = createUserRequest();

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(userRequest)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(identityService);
  }

  @Test
  void shouldReturn200WhenUserExists() throws Exception {
    when(identityService.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(getUserEntity());

    mockMvc
        .perform(
            get(URL + "/%s".formatted(USER_ID))
                .header("X-Tenant-Id", TENANT_ID)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(UserStatus.ACTIVE.name()))
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
  }

  @Test
  void shouldReturn404WhenUserDoesNotExist() throws Exception {
    var getUrl = URL + "/" + USER_ID;

    when(identityService.findByIdAndTenantId(USER_ID, TENANT_ID))
        .thenThrow(new NoSuchElementException("User"));

    mockMvc
        .perform(
            get(getUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", TENANT_ID)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.path").value(getUrl))
        .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
        .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()));
  }

  @Test
  void shouldReturn400WhenGetTenantIdHeaderIsMissing() throws Exception {

    var getUrl = URL + "/" + USER_ID;
    mockMvc
        .perform(get(getUrl).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  @Test
  void shouldReturn400WhenGetTenantIdHeaderIsInvalid() throws Exception {
    var getUrl = URL + "/" + USER_ID;
    mockMvc
        .perform(
            get(getUrl).accept(MediaType.APPLICATION_JSON).header("X-Tenant-Id", "invalid tenant"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  @Test
  void shouldReturn400WhenUserIdIsInvalid() throws Exception {
    var getUrl = URL + "/" + "invalidUserId";
    mockMvc
        .perform(get(getUrl).accept(MediaType.APPLICATION_JSON).header("X-Tenant-Id", TENANT_ID))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(identityService);
  }

  private ResultMatcher[] validationErrors(
      HttpStatus httpStatus,
      String path,
      String error,
      String message,
      String field,
      int statusCode) {
    return new ResultMatcher[] {
      status().is(httpStatus.value()),
      jsonPath("$.path").value(path),
      jsonPath("$.error").value(error),
      jsonPath("$.errors[0].message").value(message),
      jsonPath("$.errors[0].field").value(field),
      jsonPath("$.status").value(statusCode)
    };
  }

  private static Stream<Arguments> invalidRequestsCreate() {
    return Stream.of(
        Arguments.of(
            new UserRequest("invalidEmail.com", "1234", UserRole.MEMBER),
            "email",
            "must be a well-formed email address"),
        Arguments.of(
            new UserRequest("user@email.com", "", UserRole.MEMBER),
            "password",
            "must not be blank"),
        Arguments.of(new UserRequest("user@email.com", "1234", null), "role", "must not be null"));
  }

  private static UserRequest createUserRequest() {
    return new UserRequest(EMAIL, PASSWORD, UserRole.MEMBER);
  }

  private static User getUserEntity() {
    return new User(TENANT_ID, EMAIL, PASSWORD, UserRole.MEMBER);
  }
}
