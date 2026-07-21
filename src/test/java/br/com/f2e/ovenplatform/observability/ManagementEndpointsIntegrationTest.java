package br.com.f2e.ovenplatform.observability;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest(
    properties = {
      "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
      "oven.events.publication.maintenance.enabled=false"
    })
class ManagementEndpointsIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldExposeHealthWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void shouldExposeInfoWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
  }

  @Test
  void shouldExposePrometheusMetricsWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string(containsString("jvm_memory_used_bytes")))
        .andExpect(content().string(containsString("oven_events_publications_incomplete")))
        .andExpect(content().string(containsString("oven_events_publications_failed")))
        .andExpect(
            content()
                .string(containsString("oven_events_publications_oldest_incomplete_age_seconds")))
        .andExpect(content().string(containsString("oven_events_publications_resubmissions_total")))
        .andExpect(content().string(containsString("oven_events_publications_cleanup_total")));
  }

  @Test
  void shouldNotExposeDiagnosticMetricsEndpoint() throws Exception {
    mockMvc
        .perform(get("/actuator/metrics").with(user("operator")))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnUnauthorizedForBusinessEndpointWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/orders/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
  }
}
