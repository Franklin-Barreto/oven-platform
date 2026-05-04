package br.com.f2e.ovenplatform.identity.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.application.AuthService;
import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth.LoginRequest;
import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@Import(value = {TraceContext.class})
class AuthenticationControllerTest {

  private static final String URL = "/auth/login";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;
  @MockitoBean private JwtService jwtService;

  @Test
  void shouldLoginAndReturnJwtToken() throws Exception {
    when(authService.login("john@email.com", "123456")).thenReturn("jwt-token");

    var request = new LoginRequest("john@email.com", "123456");

    mockMvc
        .perform(
            post(URL).contentType(MediaType.APPLICATION_JSON).content(JsonUtils.toJson(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"));

    verify(authService).login("john@email.com", "123456");
  }

  @Test
  void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
    var request = new LoginRequest("", "");

    mockMvc
        .perform(
            post(URL).contentType(MediaType.APPLICATION_JSON).content(JsonUtils.toJson(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(authService);
  }
}
