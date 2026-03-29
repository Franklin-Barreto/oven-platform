package br.com.f2e.ovenplatform.shared.infrastructure.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> illegalArgumentHandler(
      IllegalArgumentException exception, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI()));
  }

  @ResponseStatus(code = HttpStatus.BAD_REQUEST)
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
    return ResponseEntity.badRequest()
        .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST, errors, request.getRequestURI()));
  }

  @ResponseStatus(code = HttpStatus.NOT_FOUND)
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiErrorResponse> notFoundHandler(
      NoSuchElementException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiErrorResponse.of(
                HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI()));
  }
}
