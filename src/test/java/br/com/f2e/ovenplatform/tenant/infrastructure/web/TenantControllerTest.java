package br.com.f2e.ovenplatform.tenant.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import br.com.f2e.ovenplatform.tenant.application.TenantService;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Status;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.web.dto.CreateTenantRequest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(TenantController.class)
@Import({ApiErrorResponseFactory.class})
class TenantControllerTest {

  private static final String BASE_URL = "/tenants";
  private static final String TENANT_NAME = "Pizarria bom de garfo";
  private static final UUID TENANT_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @MockitoBean private TenantService tenantService;
  @MockitoBean private JwtService jwtService;
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
  void shouldCreateTenantAndReturn201WithLocationAndBody() throws Exception {

    var content = JsonUtils.toJson(new CreateTenantRequest(TENANT_NAME, Plan.MVP));

    var tenant = getTenant();
    when(tenantService.create(TENANT_NAME, Plan.MVP)).thenReturn(tenant);

    var result =
        mockMvc
            .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(Status.ACTIVE.name()))
            .andExpect(jsonPath("$.name").value(TENANT_NAME))
            .andExpect(jsonPath("$.plan").value(Plan.MVP.name()))
            .andExpect(jsonPath("$.status").value(Status.ACTIVE.name()))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + tenant.getId());
  }

  @Test
  void shouldReturn400WhenNameIsBlank() throws Exception {
    var content = JsonUtils.toJson(new CreateTenantRequest("", Plan.MVP));

    mockMvc
        .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(content))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                "must not be blank",
                "name",
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(tenantService);
  }

  @Test
  void shouldReturnTenantAnd200WhenTenantExists() throws Exception {
    var tenant = getTenant();
    when(tenantService.findById(tenant.getId())).thenReturn(Optional.of(tenant));

    mockMvc
        .perform(get(BASE_URL + "/" + tenant.getId()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value(TENANT_NAME))
        .andExpect(jsonPath("$.plan").value(Plan.MVP.name()))
        .andExpect(jsonPath("$.status").value(Status.ACTIVE.name()));
  }

  @Test
  void shouldReturn404WhenTenantDoesNotExist() throws Exception {
    when(tenantService.findById(getTenant().getId())).thenReturn(Optional.empty());
    var getUrl = BASE_URL + "/" + getTenant().getId();

    mockMvc
        .perform(get(getUrl).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.path").value(getUrl))
        .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
        .andExpect(jsonPath("$.errors[0].code").value(ApiErrorCodes.RESOURCE_NOT_FOUND))
        .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()));
  }

  private Tenant getTenant() {
    return withId(new Tenant(TENANT_NAME, Plan.MVP), TENANT_ID);
  }
}
