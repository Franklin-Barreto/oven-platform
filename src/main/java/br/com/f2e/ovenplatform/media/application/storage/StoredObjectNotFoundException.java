package br.com.f2e.ovenplatform.media.application.storage;

public class StoredObjectNotFoundException extends RuntimeException {

  public StoredObjectNotFoundException(String objectKey) {
    super("Stored object not found: " + objectKey);
  }
}
