package br.com.f2e.ovenplatform.shared.application.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String resource, UUID id) {
    super("%s id: %s not found".formatted(resource, id));
  }

  public ResourceNotFoundException(String resource, String field, UUID id) {
    super("%s %s: %s not found".formatted(resource, field, id));
  }

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
