package br.com.f2e.ovenplatform.media.infrastructure.web.dto;

import br.com.f2e.ovenplatform.media.application.PreparedImageUpload;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PreparedImageUploadResponse(
    UUID imageId,
    URI uploadUrl,
    String method,
    Map<String, String> requiredHeaders,
    Instant expiresAt) {

  public PreparedImageUploadResponse {
    requiredHeaders = Map.copyOf(requiredHeaders);
  }

  public static PreparedImageUploadResponse from(PreparedImageUpload preparedUpload) {
    var authorization = preparedUpload.authorization();

    return new PreparedImageUploadResponse(
        preparedUpload.imageId(),
        authorization.uploadUrl(),
        authorization.method(),
        authorization.requiredHeaders(),
        authorization.expiresAt());
  }
}
