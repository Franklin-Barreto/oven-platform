package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank @Size(min = 5, message = "name must have at least 5 characters") String name) {}
