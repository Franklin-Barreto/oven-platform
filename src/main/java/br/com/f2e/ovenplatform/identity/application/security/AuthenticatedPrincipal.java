package br.com.f2e.ovenplatform.identity.application.security;

import java.util.UUID;

public interface AuthenticatedPrincipal {
  UUID userId();
}
