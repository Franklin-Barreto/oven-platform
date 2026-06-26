package br.com.f2e.ovenplatform.identity.infrastructure.security.web;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentTenantIdArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentTenantId.class)
        && UUID.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      @NonNull MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      @NonNull NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
      throw new TenantAccessDeniedException();
    }

    return authenticatedUser.tenantId();
  }
}
