package br.com.f2e.ovenplatform.identity.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_HEADER;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.application.team.CreateTenantUserCommand;
import br.com.f2e.ovenplatform.identity.application.team.DeactivateTenantMembershipCommand;
import br.com.f2e.ovenplatform.identity.application.team.ReactivateTenantMembershipCommand;
import br.com.f2e.ovenplatform.identity.application.team.ReplaceTenantMembershipRolesCommand;
import br.com.f2e.ovenplatform.identity.application.team.TenantTeamManagementDeniedException;
import br.com.f2e.ovenplatform.identity.application.team.TenantTeamService;
import br.com.f2e.ovenplatform.identity.application.team.TenantUserResult;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.ChangeTenantMembershipStatusRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.ReplaceTenantMembershipRolesRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.UserRequest;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(IdentityController.class)
class IdentityControllerTest extends AbstractControllerTest {

  private static final String EMAIL = "user.email@outlook.com";
  private static final String PASSWORD = "1234";
  private static final UUID USER_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final UUID ACTOR_USER_ID = UUID.fromString("f34e2c29-b67f-4b3f-b4b7-fc9af5f6f7d1");
  private static final String BASE_URL = "/users";

  @MockitoBean private TenantTeamService tenantTeamService;

  @Test
  void shouldCreateUserSuccessfully() throws Exception {

    var request =
        new CreateTenantUserCommand(
            TENANT_ID, ACTOR_USER_ID, EMAIL, PASSWORD, Set.of(TenantMembershipRole.ATTENDANT));
    when(tenantTeamService.createTenantUser(request)).thenReturn(tenantUserCreatedResponse());

    var userRequest = createUserRequest();

    var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(userRequest))
                    .with(
                        authenticatedTenantUser(
                            TENANT_ID,
                            ACTOR_USER_ID,
                            TenantMembershipRole.MANAGER,
                            TenantPermission.TEAM_MANAGE))
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
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_MANAGE))
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

    verifyNoInteractions(tenantTeamService);
  }

  @Test
  void shouldReturn200WhenUserExists() throws Exception {
    when(tenantTeamService.findTenantUserById(TENANT_ID, USER_ID))
        .thenReturn(tenantUserCreatedResponse());

    mockMvc
        .perform(
            get(BASE_URL + "/%s".formatted(USER_ID))
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(TenantMembershipStatus.ACTIVE.name()))
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
  }

  @Test
  void shouldReturn404WhenUserDoesNotExist() throws Exception {
    var getUrl = BASE_URL + "/" + USER_ID;

    when(tenantTeamService.findTenantUserById(TENANT_ID, USER_ID))
        .thenThrow(new NoSuchElementException("User"));

    mockMvc
        .perform(
            get(getUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_READ))
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
            get(getUrl)
                .accept(MediaType.APPLICATION_JSON)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_READ)))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                getUrl,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_ARGUMENT,
                "Invalid UUID string: invalidUserId",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(tenantTeamService);
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
        new CreateTenantUserCommand(
            TENANT_ID, ACTOR_USER_ID, EMAIL, PASSWORD, Set.of(TenantMembershipRole.ATTENDANT));
    when(tenantTeamService.createTenantUser(request)).thenThrow(exception);

    var userRequest = createUserRequest();

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(userRequest))
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_MANAGE))
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
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_READ))
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

    verifyNoInteractions(tenantTeamService);
  }

  @Test
  void shouldReturnBadRequestWhenApiVersionIsSupportedButNotAcceptedByEndpoint() throws Exception {
    var getUrl = BASE_URL + "/" + USER_ID;

    mockMvc
        .perform(
            get(getUrl)
                .accept(MediaType.APPLICATION_JSON)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_READ))
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

    verifyNoInteractions(tenantTeamService);
  }

  @Test
  void shouldListTenantMemberships() throws Exception {
    when(tenantTeamService.listTenantMemberships(TENANT_ID))
        .thenReturn(List.of(tenantUserCreatedResponse()));

    mockMvc
        .perform(
            get(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID,
                        ACTOR_USER_ID,
                        TenantMembershipRole.MANAGER,
                        TenantPermission.TEAM_READ)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(USER_ID.toString()))
        .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$[0].email").value(EMAIL));

    verify(tenantTeamService).listTenantMemberships(TENANT_ID);
  }

  @ParameterizedTest
  @MethodSource("membershipStatusChanges")
  void shouldChangeTenantMembershipStatus(
      TenantMembershipStatus membershipStatus, Consumer<TenantTeamService> verification)
      throws Exception {
    var request = new ChangeTenantMembershipStatusRequest(membershipStatus);

    mockMvc
        .perform(
            put(BASE_URL + "/" + USER_ID + "/status")
                .with(teamManager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verification.accept(verify(tenantTeamService));
  }

  @Test
  void shouldReplaceTenantMembershipRoles() throws Exception {
    var roles = Set.of(TenantMembershipRole.ATTENDANT, TenantMembershipRole.KITCHEN);
    var request = new ReplaceTenantMembershipRolesRequest(roles);
    var command = new ReplaceTenantMembershipRolesCommand(TENANT_ID, ACTOR_USER_ID, USER_ID, roles);

    mockMvc
        .perform(
            put(BASE_URL + "/" + USER_ID + "/roles")
                .with(teamManager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(tenantTeamService).replaceTenantMembershipRoles(command);
  }

  @ParameterizedTest
  @MethodSource("invalidMembershipMutationRequests")
  void shouldReturnBadRequestWhenMembershipMutationRequestIsInvalid(
      String path, Object request, String field, String message) throws Exception {
    mockMvc
        .perform(
            put(BASE_URL + "/" + USER_ID + path)
                .with(teamManager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL + "/" + USER_ID + path,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(tenantTeamService);
  }

  @Test
  void shouldReturnForbiddenWhenTeamManagementPolicyDeniesMutation() throws Exception {
    var roles = Set.of(TenantMembershipRole.MANAGER);
    var command = new ReplaceTenantMembershipRolesCommand(TENANT_ID, ACTOR_USER_ID, USER_ID, roles);
    when(tenantTeamService.replaceTenantMembershipRoles(command))
        .thenThrow(new TenantTeamManagementDeniedException());

    var path = BASE_URL + "/" + USER_ID + "/roles";
    mockMvc
        .perform(
            put(path)
                .with(teamManager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new ReplaceTenantMembershipRolesRequest(roles))))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.FORBIDDEN,
                path,
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ApiErrorCodes.TENANT_TEAM_MANAGEMENT_DENIED,
                "Actor cannot manage the target tenant membership",
                null,
                HttpStatus.FORBIDDEN.value()));
  }

  @ParameterizedTest
  @MethodSource("teamProtectedRequests")
  void shouldReturnForbiddenWhenTeamPermissionIsMissing(MockHttpServletRequestBuilder request)
      throws Exception {
    mockMvc
        .perform(
            request.with(
                authenticatedTenantUser(
                    TENANT_ID, TenantMembershipRole.KITCHEN, TenantPermission.KITCHEN_READ)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(tenantTeamService);
  }

  @Test
  void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
    mockMvc.perform(get(BASE_URL + "/" + USER_ID)).andExpect(status().isUnauthorized());

    verifyNoInteractions(tenantTeamService);
  }

  private static Stream<Arguments> teamProtectedRequests() {
    return Stream.of(
        Arguments.of(get(BASE_URL)),
        Arguments.of(get(BASE_URL + "/" + USER_ID)),
        Arguments.of(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createUserRequest()))),
        Arguments.of(
            put(BASE_URL + "/" + USER_ID + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonUtils.toJson(
                        new ChangeTenantMembershipStatusRequest(TenantMembershipStatus.INACTIVE)))),
        Arguments.of(
            put(BASE_URL + "/" + USER_ID + "/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonUtils.toJson(
                        new ReplaceTenantMembershipRolesRequest(
                            Set.of(TenantMembershipRole.ATTENDANT))))));
  }

  private static Stream<Arguments> membershipStatusChanges() {
    return Stream.of(
        Arguments.of(
            TenantMembershipStatus.ACTIVE,
            (Consumer<TenantTeamService>)
                service ->
                    service.reactivateTenantMembership(
                        new ReactivateTenantMembershipCommand(TENANT_ID, ACTOR_USER_ID, USER_ID))),
        Arguments.of(
            TenantMembershipStatus.INACTIVE,
            (Consumer<TenantTeamService>)
                service ->
                    service.deactivateTenantMembership(
                        new DeactivateTenantMembershipCommand(TENANT_ID, ACTOR_USER_ID, USER_ID))));
  }

  private static Stream<Arguments> invalidMembershipMutationRequests() {
    return Stream.of(
        Arguments.of(
            "/status", new ChangeTenantMembershipStatusRequest(null), "status", "must not be null"),
        Arguments.of(
            "/roles",
            new ReplaceTenantMembershipRolesRequest(Set.of()),
            "roles",
            "must not be empty"));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor teamManager() {
    return authenticatedTenantUser(
        TENANT_ID, ACTOR_USER_ID, TenantMembershipRole.MANAGER, TenantPermission.TEAM_MANAGE);
  }

  private static Stream<Arguments> invalidRequestsCreate() {
    return Stream.of(
        Arguments.of(
            new UserRequest("invalidEmail.com", "1234", Set.of(TenantMembershipRole.ATTENDANT)),
            "email",
            "must be a well-formed email address"),
        Arguments.of(
            new UserRequest("user@email.com", "", Set.of(TenantMembershipRole.ATTENDANT)),
            "password",
            "must not be blank"),
        Arguments.of(new UserRequest("user@email.com", "1234", null), "roles", "must not be empty"),
        Arguments.of(
            new UserRequest("user@email.com", "1234", Set.of()), "roles", "must not be empty"));
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
    return new UserRequest(EMAIL, PASSWORD, Set.of(TenantMembershipRole.ATTENDANT));
  }

  private static TenantUserResult tenantUserCreatedResponse() {
    return new TenantUserResult(
        USER_ID,
        TENANT_ID,
        EMAIL,
        Set.of(TenantMembershipRole.ATTENDANT),
        TenantMembershipStatus.ACTIVE);
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
