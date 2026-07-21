package br.com.f2e.ovenplatform.e2e.support;

import java.util.UUID;

public record SeededUser(UUID tenantId, String email, String password) {}
