package br.com.f2e.ovenplatform.media.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PrepareImageUploadRequest(
    @NotBlank String contentType, @Positive long sizeBytes, @NotBlank String checksum) {}
