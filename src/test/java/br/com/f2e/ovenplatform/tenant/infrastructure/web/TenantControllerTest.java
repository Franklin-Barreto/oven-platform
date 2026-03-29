package br.com.f2e.ovenplatform.tenant.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
    when(tenantService.create(TENANT_NAME, Plan.MVP)).thenReturn(new Tenant(TENANT_NAME, Plan.MVP));
    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(content))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/tenants/")))
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
        .andExpect(status().isBadRequest());
    verifyNoInteractions(tenantService);
  }
}
