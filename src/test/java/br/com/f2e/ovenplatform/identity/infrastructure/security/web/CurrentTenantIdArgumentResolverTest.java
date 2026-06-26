package br.com.f2e.ovenplatform.identity.infrastructure.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

class CurrentTenantIdArgumentResolverTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID USER_ID = UUID.fromString("f34e2c29-b67f-4b3f-b4b7-fc9af5f6f7d1");

  private final CurrentTenantIdArgumentResolver resolver = new CurrentTenantIdArgumentResolver();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSupportCurrentTenantIdUuidParameter() throws Exception {
    var parameter = methodParameter("methodWithCurrentTenantId", UUID.class);

    assertThat(resolver.supportsParameter(parameter)).isTrue();
  }

  @Test
  void shouldNotSupportUuidParameterWithoutCurrentTenantIdAnnotation() throws Exception {
    var parameter = methodParameter("methodWithUuidWithoutAnnotation", UUID.class);

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  void shouldNotSupportCurrentTenantIdParameterWhenTypeIsNotUuid() throws Exception {
    var parameter = methodParameter("methodWithCurrentTenantIdButWrongType", String.class);

    assertThat(resolver.supportsParameter(parameter)).isFalse();
  }

  @Test
  void shouldResolveTenantIdFromAuthenticatedUser() throws Exception {
    authenticateTenantUser();

    var parameter = methodParameter("methodWithCurrentTenantId", UUID.class);

    var result = resolve(parameter);

    assertThat(result).isEqualTo(TENANT_ID);
  }

  @Test
  void shouldThrowTenantAccessDeniedWhenAuthenticationIsMissing() throws Exception {
    var parameter = methodParameter("methodWithCurrentTenantId", UUID.class);

    assertThrows(TenantAccessDeniedException.class, () -> resolve(parameter));
  }

  @Test
  void shouldThrowTenantAccessDeniedWhenPrincipalIsNotAuthenticatedUser() throws Exception {
    var authentication =
        new UsernamePasswordAuthenticationToken("not-authenticated-user", null, List.of());

    SecurityContextHolder.getContext().setAuthentication(authentication);

    var parameter = methodParameter("methodWithCurrentTenantId", UUID.class);

    assertThrows(TenantAccessDeniedException.class, () -> resolve(parameter));
  }

  private static void authenticateTenantUser() {
    var role = TenantMembershipRole.MEMBER;
    var authenticatedUser = new AuthenticatedUser(TENANT_ID, USER_ID, role);

    var authentication =
        new UsernamePasswordAuthenticationToken(
            authenticatedUser, null, List.of(new SimpleGrantedAuthority(role.name())));

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private static MethodParameter methodParameter(String methodName, Class<?> parameterType)
      throws NoSuchMethodException {
    Method method = TestController.class.getDeclaredMethod(methodName, parameterType);
    return new MethodParameter(method, 0);
  }

  private Object resolve(MethodParameter parameter) {
    return resolver.resolveArgument(
        parameter,
        new ModelAndViewContainer(),
        mock(NativeWebRequest.class),
        mock(WebDataBinderFactory.class));
  }

  @SuppressWarnings("unused")
  private abstract static class TestController {

    abstract void methodWithCurrentTenantId(@CurrentTenantId UUID tenantId);

    abstract void methodWithUuidWithoutAnnotation(UUID tenantId);

    abstract void methodWithCurrentTenantIdButWrongType(@CurrentTenantId String tenantId);
  }
}
