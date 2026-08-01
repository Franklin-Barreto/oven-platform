package br.com.f2e.ovenplatform.catalog.infrastructure.web.option;

import jakarta.validation.constraints.NotNull;

public record ChangeOptionStatusRequest(@NotNull Boolean active) {}
