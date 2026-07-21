package br.com.f2e.ovenplatform.shared.infrastructure.web.test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.identity.application.TenantMembershipAuthenticationService;
import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.identity.infrastructure.security.config.SecurityConfig;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ApiErrorResponseFactory.class, WebSecurityTestConfig.class, SecurityConfig.class})
public abstract class AbstractControllerTest {

  protected static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");

  @Autowired protected MockMvc mockMvc;

  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private PasswordEncoder passwordEncoder;
  @MockitoBean private Tracer tracer;
  @MockitoBean private Span span;
  @MockitoBean private TraceContext traceContext;
  @MockitoBean private TenantMembershipAuthenticationService membershipAuthenticationService;

  @BeforeEach
  void setUpTracing() {
    doReturn(span).when(tracer).currentSpan();
    when(span.context()).thenReturn(traceContext);
    when(traceContext.traceId()).thenReturn("abc-123");
  }
}
