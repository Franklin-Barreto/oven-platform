package br.com.f2e.ovenplatform.payment.application.exception;

public class ActiveAttemptAlreadyExistsException extends RuntimeException {

  public ActiveAttemptAlreadyExistsException() {
    super("Active attempt already exists");
  }

  public ActiveAttemptAlreadyExistsException(Throwable cause) {
    super("Active attempt already exists", cause);
  }
}
