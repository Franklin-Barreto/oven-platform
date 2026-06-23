package br.com.f2e.ovenplatform.identity.infrastructure.security.tenant;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;

import br.com.f2e.ovenplatform.identity.domain.exception.TenantAccessDeniedException;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantAccessInterceptor implements HandlerInterceptor {

  // HandlerInterceptor continues the chain unless tenant access is denied by exception.
  @SuppressWarnings("java:S3516")
  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    var tenantHeader = request.getHeader(TENANT_ID_HEADER);

    if (tenantHeader == null) {
      return true;
    }

    var authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
      return true;
    }

    var requestTenantId = UUID.fromString(tenantHeader);

    if (!requestTenantId.equals(user.tenantId())) {
      throw new TenantAccessDeniedException();
    }

    return true;
  }
}
