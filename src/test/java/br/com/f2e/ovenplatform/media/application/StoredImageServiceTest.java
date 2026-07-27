package br.com.f2e.ovenplatform.media.application;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.media.application.storage.ImageReadAuthorization;
import br.com.f2e.ovenplatform.media.application.storage.ImageStorage;
import br.com.f2e.ovenplatform.media.application.storage.ImageUploadAuthorization;
import br.com.f2e.ovenplatform.media.application.storage.StoredObjectMetadata;
import br.com.f2e.ovenplatform.media.application.storage.UploadAuthorizationSpec;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.media.domain.StoredImageMetadataMismatchException;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class StoredImageServiceTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID IMAGE_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final String CONTENT_TYPE = "image/webp";
  private static final long SIZE_BYTES = 42_000L;
  private static final String CHECKSUM = "sha256:8b1a9953c4611296a827abf8c47804d7";
  private static final String OBJECT_KEY =
      "tenants/%s/images/7d877954-28f7-483d-9c21-60d13ec17e80.webp".formatted(TENANT_ID);
  private static final Instant EXPIRES_AT = Instant.parse("2026-07-27T15:10:00Z");

  @Mock private StoredImageRepository repository;
  @Mock private ImageStorage imageStorage;

  private StoredImageService service;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    service =
        new StoredImageService(
            repository, imageStorage, new MediaProperties(DataSize.ofMegabytes(10)));
  }

  @Test
  void shouldCreatePendingImageAndAuthorizeDirectUpload() {
    var command = new PrepareImageUploadCommand(CONTENT_TYPE, SIZE_BYTES, CHECKSUM);
    var authorization =
        new ImageUploadAuthorization(
            URI.create("https://storage.example/upload"),
            "PUT",
            Map.of("Content-Type", CONTENT_TYPE),
            EXPIRES_AT);

    when(repository.save(any(StoredImage.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), IMAGE_ID));
    when(imageStorage.authorizeUpload(any(UploadAuthorizationSpec.class)))
        .thenReturn(authorization);

    var result = service.prepareUpload(TENANT_ID, command);

    var imageCaptor = ArgumentCaptor.forClass(StoredImage.class);
    verify(repository).save(imageCaptor.capture());
    var image = imageCaptor.getValue();

    assertThat(image.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(image.getContentType()).isEqualTo(CONTENT_TYPE);
    assertThat(image.getSizeBytes()).isEqualTo(SIZE_BYTES);
    assertThat(image.getChecksum()).isEqualTo(CHECKSUM);
    assertThat(image.isAvailable()).isFalse();
    assertThat(image.getObjectKey())
        .startsWith("tenants/%s/images/".formatted(TENANT_ID))
        .endsWith(".webp");

    verify(imageStorage)
        .authorizeUpload(
            new UploadAuthorizationSpec(image.getObjectKey(), CONTENT_TYPE, SIZE_BYTES, CHECKSUM));
    assertThat(result.imageId()).isEqualTo(IMAGE_ID);
    assertThat(result.authorization()).isEqualTo(authorization);

    var ordered = inOrder(repository, imageStorage);
    ordered.verify(repository).save(image);
    ordered.verify(imageStorage).authorizeUpload(any(UploadAuthorizationSpec.class));
  }

  @Test
  void shouldRejectImageLargerThanConfiguredMaximum() {
    var command =
        new PrepareImageUploadCommand(
            CONTENT_TYPE, DataSize.ofMegabytes(10).toBytes() + 1, CHECKSUM);

    assertThatThrownBy(() -> service.prepareUpload(TENANT_ID, command))
        .isInstanceOf(ImageUploadSizeExceededException.class)
        .hasMessage("Image size 10485761 bytes exceeds the maximum of 10485760 bytes");

    verifyNoInteractions(repository, imageStorage);
  }

  @Test
  void shouldConfirmUploadWhenStoredMetadataMatchesExpectedMetadata() {
    var image = pendingImage();
    var metadata = new StoredObjectMetadata(CONTENT_TYPE, SIZE_BYTES, CHECKSUM);
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID)).thenReturn(Optional.of(image));
    when(imageStorage.getMetadata(OBJECT_KEY)).thenReturn(metadata);
    when(repository.save(image)).thenReturn(image);

    var confirmed = service.confirmUpload(TENANT_ID, IMAGE_ID);

    assertThat(confirmed).isSameAs(image);
    assertThat(confirmed.isAvailable()).isTrue();
    var ordered = inOrder(imageStorage, repository);
    ordered.verify(imageStorage).getMetadata(OBJECT_KEY);
    ordered.verify(repository).save(image);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("incompatibleMetadata")
  void shouldRejectConfirmationWhenStoredMetadataDoesNotMatch(
      String scenario, StoredObjectMetadata metadata) {
    var image = pendingImage();
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID)).thenReturn(Optional.of(image));
    when(imageStorage.getMetadata(OBJECT_KEY)).thenReturn(metadata);

    assertThatThrownBy(() -> service.confirmUpload(TENANT_ID, IMAGE_ID))
        .isInstanceOf(StoredImageMetadataMismatchException.class);

    assertThat(image.isAvailable()).isFalse();
    verify(repository, never()).save(any());
  }

  @Test
  void shouldTreatConfirmationOfAvailableImageAsIdempotent() {
    var image = availableImage();
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID)).thenReturn(Optional.of(image));

    var confirmed = service.confirmUpload(TENANT_ID, IMAGE_ID);

    assertThat(confirmed).isSameAs(image);
    verifyNoInteractions(imageStorage);
    verify(repository, never()).save(any());
  }

  @Test
  void shouldNotRevealWhetherImageExistsForAnotherTenant() {
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirmUpload(TENANT_ID, IMAGE_ID))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("StoredImage id: %s not found".formatted(IMAGE_ID));

    verifyNoInteractions(imageStorage);
  }

  @Test
  void shouldAuthorizeReadForAvailableImage() {
    var image = availableImage();
    var authorization =
        new ImageReadAuthorization(
            URI.create("https://storage.example/read/image.webp"), EXPIRES_AT);
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID)).thenReturn(Optional.of(image));
    when(imageStorage.authorizeRead(OBJECT_KEY)).thenReturn(authorization);

    var result = service.authorizeRead(TENANT_ID, IMAGE_ID);

    assertThat(result).isEqualTo(authorization);
    verify(imageStorage).authorizeRead(OBJECT_KEY);
  }

  @Test
  void shouldRejectReadAuthorizationForPendingImage() {
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID))
        .thenReturn(Optional.of(pendingImage()));

    assertThatThrownBy(() -> service.authorizeRead(TENANT_ID, IMAGE_ID))
        .isInstanceOf(StoredImageNotAvailableException.class);

    verifyNoInteractions(imageStorage);
  }

  @Test
  void shouldDeleteStorageObjectBeforeDeletingDatabaseRecord() {
    var image = pendingImage();
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID)).thenReturn(Optional.of(image));

    service.delete(TENANT_ID, IMAGE_ID);

    var ordered = inOrder(imageStorage, repository);
    ordered.verify(imageStorage).delete(OBJECT_KEY);
    ordered.verify(repository).delete(image);
  }

  @Test
  void shouldKeepDatabaseRecordWhenStorageDeletionFails() {
    var image = pendingImage();
    when(repository.findByIdAndTenantId(IMAGE_ID, TENANT_ID)).thenReturn(Optional.of(image));
    var storageFailure = new IllegalStateException("storage unavailable");
    doThrow(storageFailure).when(imageStorage).delete(OBJECT_KEY);

    assertThatThrownBy(() -> service.delete(TENANT_ID, IMAGE_ID)).isSameAs(storageFailure);

    verify(repository, never()).delete(any());
  }

  private static Stream<Arguments> incompatibleMetadata() {
    return Stream.of(
        Arguments.of(
            "different content type", new StoredObjectMetadata("image/png", SIZE_BYTES, CHECKSUM)),
        Arguments.of(
            "different size", new StoredObjectMetadata(CONTENT_TYPE, SIZE_BYTES + 1, CHECKSUM)),
        Arguments.of(
            "different checksum",
            new StoredObjectMetadata(CONTENT_TYPE, SIZE_BYTES, "sha256:different")));
  }

  private static StoredImage pendingImage() {
    return withId(
        StoredImage.pending(TENANT_ID, OBJECT_KEY, CONTENT_TYPE, SIZE_BYTES, CHECKSUM), IMAGE_ID);
  }

  private static StoredImage availableImage() {
    var image = pendingImage();
    image.confirm(CONTENT_TYPE, SIZE_BYTES, CHECKSUM);
    return image;
  }
}
