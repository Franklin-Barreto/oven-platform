package br.com.f2e.ovenplatform.identity.application.authentication;

import java.util.UUID;

public interface AccessTokenService {
  String generateToken(UUID tenantId, UUID userId);
}
