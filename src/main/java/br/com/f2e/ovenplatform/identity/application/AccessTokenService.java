package br.com.f2e.ovenplatform.identity.application;

import java.util.UUID;

public interface AccessTokenService {
  String generateToken(UUID userId, String role);
}
