package br.com.f2e.ovenplatform.media.application;

public class ImageUploadSizeExceededException extends RuntimeException {

  public ImageUploadSizeExceededException(long actualSizeBytes, long maximumSizeBytes) {
    super(
        "Image size %d bytes exceeds the maximum of %d bytes"
            .formatted(actualSizeBytes, maximumSizeBytes));
  }
}
