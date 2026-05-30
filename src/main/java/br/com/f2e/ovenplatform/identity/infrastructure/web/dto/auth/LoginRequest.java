package br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LoginRequest(
    @NotNull UUID tenantId,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password) {}
