package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.accept.NotAcceptableApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> illegalArgumentHandler(
      IllegalArgumentException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> badRequestHandler(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    var errors =
        exception.getFieldErrors().stream()
            .map(
                error -> {
                  String message = messageSource.getMessage(error, LocaleContextHolder.getLocale());
                  return ApiErrorItem.of(error.getField(), message);
                })
            .toList();
    return error(HttpStatus.BAD_REQUEST, errors, request);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiErrorResponse> notFoundHandler(
      NoSuchElementException exception, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> dataIntegrityViolationHandler(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    return switch (getConstraintName(exception)) {
      case "uk_users_tenant_id_email" ->
          error(HttpStatus.CONFLICT, "A user with this email already exists.", request);

      case "fk_users_tenant_id" -> error(HttpStatus.NOT_FOUND, "Tenant not found.", request);

      case null, default -> error(HttpStatus.CONFLICT, "Data integrity violation.", request);
    };
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ApiErrorResponse> missingHeaderHandler(
      MissingRequestHeaderException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(NotAcceptableApiVersionException.class)
  public ResponseEntity<ApiErrorResponse> notAcceptableApiVersionHandler(
      NotAcceptableApiVersionException exception, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  private String getConstraintName(Throwable ex) {
    Throwable cause = ex;
    while (cause != null) {
      if (cause instanceof PSQLException psqlEx) {
        return Optional.ofNullable(psqlEx.getServerErrorMessage())
            .map(ServerErrorMessage::getConstraint)
            .orElse(null);
      }
      cause = cause.getCause();
    }
    return null;
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(status, message, request.getRequestURI()));
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status, List<ApiErrorItem> errors, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(status, errors, request.getRequestURI()));
  }
}
