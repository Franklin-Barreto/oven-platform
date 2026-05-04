package br.com.f2e.ovenplatform.identity.infrastructure.security.dto;

import br.com.f2e.ovenplatform.identity.domain.UserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, UserRole role) {}
