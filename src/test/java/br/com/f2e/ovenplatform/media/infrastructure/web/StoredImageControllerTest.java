package br.com.f2e.ovenplatform.media.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.media.application.ImageUploadSizeExceededException;
import br.com.f2e.ovenplatform.media.application.PrepareImageUploadCommand;
import br.com.f2e.ovenplatform.media.application.PreparedImageUpload;
import br.com.f2e.ovenplatform.media.application.StoredImageNotAvailableException;
import br.com.f2e.ovenplatform.media.application.StoredImageService;
import br.com.f2e.ovenplatform.media.application.delivery.PublicImageLocation;
import br.com.f2e.ovenplatform.media.application.storage.ImageUploadAuthorization;
import br.com.f2e.ovenplatform.media.domain.StoredImageMetadataMismatchException;
import br.com.f2e.ovenplatform.media.infrastructure.web.dto.PrepareImageUploadRequest;
import br.com.f2e.ovenplatform.media.infrastructure.web.exception.MediaExceptionHandler;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = StoredImageController.class)
@Import(MediaExceptionHandler.class)
class StoredImageControllerTest extends AbstractControllerTest {

  private static final String BASE_URL = "/images";
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID IMAGE_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final String CONTENT_TYPE = "image/webp";
  private static final long SIZE_BYTES = 42_000L;
  private static final String CHECKSUM = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";
  private static final String OBJECT_KEY =
      "tenants/%s/images/7d877954-28f7-483d-9c21-60d13ec17e80.webp".formatted(TENANT_ID);
  private static final Instant EXPIRES_AT = Instant.parse("2026-07-27T15:10:00Z");

  @MockitoBean private StoredImageService service;

  @Test
  void shouldPrepareUploadSuccessfully() throws Exception {
    var request =
        JsonUtils.toJson(new PrepareImageUploadRequest(CONTENT_TYPE, SIZE_BYTES, CHECKSUM));
    var uploadUrl = URI.create("https://oven-platform-test-images.s3.amazonaws.com/" + OBJECT_KEY);

    var authorization =
        new ImageUploadAuthorization(
            uploadUrl,
            "PUT",
            Map.of(
                "content-type", CONTENT_TYPE,
                "x-amz-checksum-sha256", CHECKSUM),
            EXPIRES_AT);

    var preparedUpload = new PreparedImageUpload(IMAGE_ID, authorization);

    when(service.prepareUpload(
            TENANT_ID, new PrepareImageUploadCommand(CONTENT_TYPE, SIZE_BYTES, CHECKSUM)))
        .thenReturn(preparedUpload);

    mockMvc
        .perform(
            post(BASE_URL + "/uploads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .with(authenticatedUser(TenantPermission.CATALOG_MANAGE)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.imageId").value(IMAGE_ID.toString()))
        .andExpect(jsonPath("$.uploadUrl").value(uploadUrl.toString()))
        .andExpect(jsonPath("$.method").value("PUT"))
        .andExpect(jsonPath("$.requiredHeaders['content-type']").value(CONTENT_TYPE))
        .andExpect(jsonPath("$.requiredHeaders['x-amz-checksum-sha256']").value(CHECKSUM))
        .andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT.toString()));

    verify(service)
        .prepareUpload(
            TENANT_ID, new PrepareImageUploadCommand(CONTENT_TYPE, SIZE_BYTES, CHECKSUM));
  }

  @Test
  void shouldReturn413WhenImageUploadExceedsMaximumSize() throws Exception {
    var request =
        JsonUtils.toJson(new PrepareImageUploadRequest(CONTENT_TYPE, SIZE_BYTES, CHECKSUM));

    when(service.prepareUpload(
            TENANT_ID, new PrepareImageUploadCommand(CONTENT_TYPE, SIZE_BYTES, CHECKSUM)))
        .thenThrow(new ImageUploadSizeExceededException(SIZE_BYTES, 20000L));

    var fullPath = BASE_URL + "/uploads";
    mockMvc
        .perform(
            post(fullPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .with(authenticatedUser(TenantPermission.CATALOG_MANAGE)))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.CONTENT_TOO_LARGE,
                fullPath,
                HttpStatus.CONTENT_TOO_LARGE.getReasonPhrase(),
                ApiErrorCodes.IMAGE_UPLOAD_SIZE_EXCEEDED,
                "Image size %d bytes exceeds the maximum of 20000 bytes".formatted(SIZE_BYTES),
                null,
                HttpStatus.CONTENT_TOO_LARGE.value()));
  }

  @Test
  void shouldConfirmUploadSuccessfully() throws Exception {

    var fullPath = BASE_URL + "/%s/confirmation".formatted(IMAGE_ID.toString());

    mockMvc
        .perform(put(fullPath).with(authenticatedUser(TenantPermission.CATALOG_MANAGE)))
        .andExpect(status().isNoContent());

    verify(service).confirmUpload(TENANT_ID, IMAGE_ID);
  }

  @Test
  void shouldResolvePublicLocationSuccessfully() throws Exception {

    var publicUrl = URI.create("https://media.oven-platform.com/" + OBJECT_KEY);

    when(service.resolvePublicLocation(TENANT_ID, IMAGE_ID))
        .thenReturn(new PublicImageLocation(publicUrl));

    var fullPath = BASE_URL + "/%s/location".formatted(IMAGE_ID.toString());

    mockMvc
        .perform(get(fullPath).with(authenticatedUser(TenantPermission.CATALOG_READ)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value(publicUrl.toString()));

    verify(service).resolvePublicLocation(TENANT_ID, IMAGE_ID);
  }

  @Test
  void shouldDeleteStoredImageSuccessfully() throws Exception {

    var fullPath = BASE_URL + "/" + IMAGE_ID;

    mockMvc
        .perform(delete(fullPath).with(authenticatedUser(TenantPermission.CATALOG_MANAGE)))
        .andExpect(status().isNoContent());

    verify(service).delete(TENANT_ID, IMAGE_ID);
  }

  @ParameterizedTest
  @MethodSource("invalidPrepareUploadRequests")
  void shouldReturn400WhenPrepareUploadRequestIsInvalid(
      PrepareImageUploadRequest request, String field, String message) throws Exception {

    var fullPath = BASE_URL + "/uploads";
    mockMvc
        .perform(
            post(fullPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .with(authenticatedUser(TenantPermission.CATALOG_MANAGE)))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                fullPath,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @MethodSource("requestsWithoutRequiredPermission")
  void shouldReturn403WithoutRequiredPermission(MockHttpServletRequestBuilder request)
      throws Exception {
    mockMvc
        .perform(request.with(authenticatedTenantUser(TENANT_ID, TenantMembershipRole.MANAGER)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }

  @Test
  void shouldReturn409WhenUploadedObjectMetadataDoesNotMatch() throws Exception {

    var fullPath = BASE_URL + "/%s/confirmation".formatted(IMAGE_ID.toString());

    when(service.confirmUpload(TENANT_ID, IMAGE_ID))
        .thenThrow(new StoredImageMetadataMismatchException());

    mockMvc
        .perform(put(fullPath).with(authenticatedUser(TenantPermission.CATALOG_MANAGE)))
        .andExpect(status().isConflict())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.CONFLICT,
                fullPath,
                HttpStatus.CONFLICT.getReasonPhrase(),
                ApiErrorCodes.STORED_IMAGE_METADATA_MISMATCH,
                "Stored image metadata does not match the expected upload metadata",
                null,
                HttpStatus.CONFLICT.value()));

    verify(service).confirmUpload(TENANT_ID, IMAGE_ID);
  }

  @Test
  void shouldReturn409WhenStoredImageIsNotAvailable() throws Exception {

    when(service.resolvePublicLocation(TENANT_ID, IMAGE_ID))
        .thenThrow(new StoredImageNotAvailableException());

    var fullPath = BASE_URL + "/%s/location".formatted(IMAGE_ID.toString());

    mockMvc
        .perform(get(fullPath).with(authenticatedUser(TenantPermission.CATALOG_READ)))
        .andExpect(status().isConflict())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.CONFLICT,
                fullPath,
                HttpStatus.CONFLICT.getReasonPhrase(),
                ApiErrorCodes.STORED_IMAGE_NOT_AVAILABLE,
                "Image is pending",
                null,
                HttpStatus.CONFLICT.value()));

    verify(service).resolvePublicLocation(TENANT_ID, IMAGE_ID);
  }

  @Test
  void shouldReturn404WhenStoredImageDoesNotExist() throws Exception {

    var exception = new ResourceNotFoundException("StoredImage", IMAGE_ID);
    when(service.resolvePublicLocation(TENANT_ID, IMAGE_ID)).thenThrow(exception);

    var fullPath = BASE_URL + "/%s/location".formatted(IMAGE_ID.toString());

    mockMvc
        .perform(get(fullPath).with(authenticatedUser(TenantPermission.CATALOG_READ)))
        .andExpect(status().isNotFound())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.NOT_FOUND,
                fullPath,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ApiErrorCodes.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                null,
                HttpStatus.NOT_FOUND.value()));

    verify(service).resolvePublicLocation(TENANT_ID, IMAGE_ID);
  }

  private static Stream<MockHttpServletRequestBuilder> requestsWithoutRequiredPermission() {
    return Stream.of(
        post(BASE_URL + "/uploads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(validPrepareUploadRequest())),
        put(BASE_URL + "/" + IMAGE_ID + "/confirmation"),
        get(BASE_URL + "/" + IMAGE_ID + "/location"),
        delete(BASE_URL + "/" + IMAGE_ID));
  }

  private static Stream<Arguments> invalidPrepareUploadRequests() {
    return Stream.of(
        Arguments.of(
            new PrepareImageUploadRequest("", SIZE_BYTES, CHECKSUM),
            "contentType",
            "must not be blank"),
        Arguments.of(
            new PrepareImageUploadRequest(CONTENT_TYPE, 0, CHECKSUM),
            "sizeBytes",
            "must be greater than 0"),
        Arguments.of(
            new PrepareImageUploadRequest(CONTENT_TYPE, SIZE_BYTES, ""),
            "checksum",
            "must not be blank"));
  }

  private static @NotNull RequestPostProcessor authenticatedUser(TenantPermission permission) {
    return authenticatedTenantUser(TENANT_ID, TenantMembershipRole.MANAGER, permission);
  }

  private static PrepareImageUploadRequest validPrepareUploadRequest() {
    return new PrepareImageUploadRequest(CONTENT_TYPE, SIZE_BYTES, CHECKSUM);
  }
}
