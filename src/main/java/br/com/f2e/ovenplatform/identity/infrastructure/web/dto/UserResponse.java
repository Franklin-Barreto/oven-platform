package br.com.f2e.ovenplatform.identity.infrastructure.web.dto;

import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.identity.domain.UserRole;
import br.com.f2e.ovenplatform.identity.domain.UserStatus;
import java.util.UUID;

public record UserResponse(UUID id, UUID tenantId, String email, UserRole role, UserStatus status) {

  public static UserResponse fromEntity(User user) {
    return new UserResponse(
        user.getId(), user.getTenantId(), user.getEmail(), user.getRole(), user.getStatus());
  }
}
