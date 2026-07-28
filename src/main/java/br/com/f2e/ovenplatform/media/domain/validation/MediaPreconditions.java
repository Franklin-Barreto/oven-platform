package br.com.f2e.ovenplatform.media.domain.validation;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

import java.util.Base64;

public final class MediaPreconditions {

  private static final int SHA_256_SIZE_BYTES = 32;

  private MediaPreconditions() {}

  public static String requireSha256Checksum(String checksum) {
    var normalized = requireNotBlank(checksum, "checksum");

    try {
      var decoded = Base64.getDecoder().decode(normalized);
      var canonical = Base64.getEncoder().encodeToString(decoded);

      if (decoded.length != SHA_256_SIZE_BYTES || !canonical.equals(normalized)) {
        throw invalidChecksum();
      }

      return normalized;
    } catch (IllegalArgumentException _) {
      throw invalidChecksum();
    }
  }

  private static IllegalArgumentException invalidChecksum() {
    return new IllegalArgumentException("checksum must be a Base64-encoded SHA-256 digest");
  }
}
