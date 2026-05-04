package br.com.f2e.ovenplatform.identity.infrastructure.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email String email, @NotBlank @Size(min = 6) String password) {}
