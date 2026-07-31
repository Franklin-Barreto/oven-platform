package br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup;

import jakarta.validation.constraints.NotNull;

public record ChangeOptionGroupStatusRequest(@NotNull Boolean active) {}
