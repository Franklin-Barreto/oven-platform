package br.com.f2e.ovenplatform.media.application.storage;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public record ImageUploadAuthorization(
    URI uploadUrl, String method, Map<String, String> requiredHeaders, Instant expiresAt) {

  public ImageUploadAuthorization {
    requireNotNull(uploadUrl, "uploadUrl");
    method = requireNotBlank(method, "method");
    requiredHeaders = Map.copyOf(requireNotNull(requiredHeaders, "requiredHeaders"));
    requireNotNull(expiresAt, "expiresAt");
  }
}
