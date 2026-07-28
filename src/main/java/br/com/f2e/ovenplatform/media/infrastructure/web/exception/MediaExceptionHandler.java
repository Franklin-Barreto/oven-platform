package br.com.f2e.ovenplatform.media.infrastructure.web.exception;

import br.com.f2e.ovenplatform.media.application.ImageUploadSizeExceededException;
import br.com.f2e.ovenplatform.media.application.StoredImageNotAvailableException;
import br.com.f2e.ovenplatform.media.application.storage.StoredObjectNotFoundException;
import br.com.f2e.ovenplatform.media.domain.StoredImageMetadataMismatchException;
import br.com.f2e.ovenplatform.media.infrastructure.web.StoredImageController;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StoredImageController.class)
public class MediaExceptionHandler {

  private final ApiErrorResponseFactory responseFactory;

  public MediaExceptionHandler(ApiErrorResponseFactory responseFactory) {
    this.responseFactory = responseFactory;
  }

  @ExceptionHandler(ImageUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> imageUploadSizeExceeded(
      ImageUploadSizeExceededException exception, HttpServletRequest request) {

    return responseFactory.create(
        HttpStatus.CONTENT_TOO_LARGE,
        ApiErrorCodes.IMAGE_UPLOAD_SIZE_EXCEEDED,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(StoredImageMetadataMismatchException.class)
  public ResponseEntity<ApiErrorResponse> storedImageMetadataMismatched(
      StoredImageMetadataMismatchException exception, HttpServletRequest request) {

    return responseFactory.create(
        HttpStatus.CONFLICT,
        ApiErrorCodes.STORED_IMAGE_METADATA_MISMATCH,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(StoredImageNotAvailableException.class)
  public ResponseEntity<ApiErrorResponse> storedImageUnavailable(
      StoredImageNotAvailableException exception, HttpServletRequest request) {

    return responseFactory.create(
        HttpStatus.CONFLICT,
        ApiErrorCodes.STORED_IMAGE_NOT_AVAILABLE,
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(StoredObjectNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> storedObjectNotFound(
      StoredObjectNotFoundException exception, HttpServletRequest request) {

    return responseFactory.create(
        HttpStatus.CONFLICT, ApiErrorCodes.IMAGE_UPLOAD_NOT_FOUND, exception.getMessage(), request);
  }
}
