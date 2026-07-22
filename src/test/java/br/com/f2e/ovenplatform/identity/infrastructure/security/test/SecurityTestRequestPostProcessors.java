package br.com.f2e.ovenplatform.identity.infrastructure.security.test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
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
    return authenticatedTenantUser(
        tenantId, DEFAULT_USER_ID, TenantMembershipRole.ATTENDANT, TenantPermission.ORDER_READ);
  }

  public static RequestPostProcessor authenticatedTenantUser(
      UUID tenantId, TenantMembershipRole role, TenantPermission... permissions) {
    return authenticatedTenantUser(tenantId, DEFAULT_USER_ID, role, permissions);
  }

  public static RequestPostProcessor authenticatedTenantUser(
      UUID tenantId, UUID userId, TenantMembershipRole role, TenantPermission... permissions) {
    return request -> {
      var permissionSet = Set.of(permissions);
      var authenticatedUser = new AuthenticatedUser(tenantId, userId, Set.of(role), permissionSet);

      var authentication =
          new UsernamePasswordAuthenticationToken(
              authenticatedUser,
              null,
              permissionSet.stream()
                  .map(permission -> new SimpleGrantedAuthority(permission.name()))
                  .toList());

      SecurityContextHolder.getContext().setAuthentication(authentication);

      return authentication(authentication).postProcessRequest(request);
    };
  }
}
