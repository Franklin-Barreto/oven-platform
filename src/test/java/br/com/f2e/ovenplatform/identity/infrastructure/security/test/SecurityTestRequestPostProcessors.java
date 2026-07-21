package br.com.f2e.ovenplatform.identity.infrastructure.security.test;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class SecurityTestRequestPostProcessors {

  private static final UUID DEFAULT_USER_ID =
      UUID.fromString("f34e2c29-b67f-4b3f-b4b7-fc9af5f6f7d1");

  private SecurityTestRequestPostProcessors() {}

  public static RequestPostProcessor authenticatedTenantUser(UUID tenantId) {
    return authenticatedTenantUser(tenantId, DEFAULT_USER_ID, TenantMembershipRole.ATTENDANT);
  }

  public static RequestPostProcessor authenticatedTenantUser(
      UUID tenantId, UUID userId, TenantMembershipRole role) {
    return request -> {
      var authenticatedUser =
          new AuthenticatedUser(
              tenantId, userId, Set.of(role), Set.of(TenantPermission.ORDER_READ));

      var authentication =
          new UsernamePasswordAuthenticationToken(
              authenticatedUser, null, List.of(new SimpleGrantedAuthority(role.name())));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      return request;
    };
  }
}
