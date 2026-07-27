package br.com.f2e.ovenplatform.media.domain;

public class StoredImageMetadataMismatchException extends RuntimeException {

  public StoredImageMetadataMismatchException() {
    super("Stored image metadata does not match the expected upload metadata");
  }
}
