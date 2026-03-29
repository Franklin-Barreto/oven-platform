package br.com.f2e.ovenplatform.tenant.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import br.com.f2e.ovenplatform.tenant.application.TenantService;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Status;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.web.dto.CreateTenantRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TenantController.class)
class TenantControllerTest {

  private static final String URL = "/tenants";
  public static final String TENANT_NAME = "Pizarria bom de garfo";
  @Autowired private MockMvc mockMvc;
  @MockitoBean private TenantService tenantService;

  @Test
  void shouldCreateTenantAndReturn201WithLocationAndBody() throws Exception {
    var content = JsonUtils.toJson(new CreateTenantRequest(TENANT_NAME, Plan.MVP));
    when(tenantService.create(TENANT_NAME, Plan.MVP)).thenReturn(getTenant());
    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(content))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString(URL)))
        .andExpect(jsonPath("$.status").value(Status.ACTIVE.name()))
        .andExpect(jsonPath("$.name").value(TENANT_NAME))
        .andExpect(jsonPath("$.plan").value(Plan.MVP.name()))
        .andExpect(jsonPath("$.status").value(Status.ACTIVE.name()));
  }

  @Test
  void shouldReturn400WhenNameIsBlank() throws Exception {
    var content = JsonUtils.toJson(new CreateTenantRequest("", Plan.MVP));
    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.path").value(URL))
        .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
        .andExpect(jsonPath("$.errors[0].message").value("must not be blank"))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
    verifyNoInteractions(tenantService);
  }

  @Test
  void shouldReturnTenantAnd200WhenTenantExists() throws Exception {
    var tenant = getTenant();
    when(tenantService.findById(tenant.getId())).thenReturn(Optional.of(tenant));
    mockMvc
        .perform(get(URL + "/" + tenant.getId()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value(TENANT_NAME))
        .andExpect(jsonPath("$.plan").value(Plan.MVP.name()))
        .andExpect(jsonPath("$.status").value(Status.ACTIVE.name()));
  }

  @Test
  void shouldReturn404WhenTenantDoesNotExist() throws Exception {
    when(tenantService.findById(getTenant().getId())).thenReturn(Optional.empty());
    var getUrl = URL + "/" + getTenant().getId();
    mockMvc
        .perform(get(getUrl).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.path").value(getUrl))
        .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
        .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()));
  }

  private Tenant getTenant() {
    return new Tenant(TENANT_NAME, Plan.MVP);
  }
}
