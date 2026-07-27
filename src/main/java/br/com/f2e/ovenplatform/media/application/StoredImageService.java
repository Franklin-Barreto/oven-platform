package br.com.f2e.ovenplatform.media.application;

import br.com.f2e.ovenplatform.media.application.storage.ImageReadAuthorization;
import br.com.f2e.ovenplatform.media.application.storage.ImageStorage;
import br.com.f2e.ovenplatform.media.application.storage.UploadAuthorizationSpec;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class StoredImageService {

  private static final String RESOURCE = "StoredImage";
  private final StoredImageRepository repository;
  private final ImageStorage imageStorage;
  private final MediaProperties properties;

  public StoredImageService(
      StoredImageRepository repository, ImageStorage imageStorage, MediaProperties properties) {
    this.repository = repository;
    this.imageStorage = imageStorage;
    this.properties = properties;
  }

  @Transactional
  public PreparedImageUpload prepareUpload(UUID tenantId, PrepareImageUploadCommand command) {

    validateUploadSize(command.sizeBytes());
    var objectKey = generateObjectKey(tenantId, command.contentType());
    var pendingStoreImage =
        StoredImage.pending(
            tenantId, objectKey, command.contentType(), command.sizeBytes(), command.checksum());
    var saved = repository.save(pendingStoreImage);
    var authorization =
        imageStorage.authorizeUpload(
            new UploadAuthorizationSpec(
                objectKey, command.contentType(), command.sizeBytes(), command.checksum()));

    return new PreparedImageUpload(saved.getId(), authorization);
  }

  @Transactional
  public StoredImage confirmUpload(UUID tenantId, UUID imageId) {
    var image = getImage(tenantId, imageId);

    if (image.isAvailable()) {
      return image;
    }

    var metadata = imageStorage.getMetadata(image.getObjectKey());

    image.confirm(metadata.contentType(), metadata.sizeBytes(), metadata.checksum());

    return repository.save(image);
  }

  public ImageReadAuthorization authorizeRead(UUID tenantId, UUID imageId) {
    var image = getImage(tenantId, imageId);
    if (!image.isAvailable()) {
      throw new StoredImageNotAvailableException("Image is pending");
    }
    return imageStorage.authorizeRead(image.getObjectKey());
  }

  @Transactional
  public void delete(UUID tenantId, UUID imageId) {
    var image = getImage(tenantId, imageId);
    imageStorage.delete(image.getObjectKey());
    repository.delete(image);
  }

  private StoredImage getImage(UUID tenantId, UUID imageId) {
    return repository
        .findByIdAndTenantId(imageId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, imageId));
  }

  private void validateUploadSize(long sizeBytes) {
    var maximumSizeBytes = properties.maxUploadSizeBytes();

    if (sizeBytes > maximumSizeBytes) {
      throw new ImageUploadSizeExceededException(sizeBytes, maximumSizeBytes);
    }
  }

  private String generateObjectKey(UUID tenantId, String contentType) {
    var objectId = UUID.randomUUID();
    var extension = extensionFor(contentType);

    return "tenants/%s/images/%s.%s".formatted(tenantId, objectId, extension);
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default ->
          throw new IllegalArgumentException("Unsupported image content type: " + contentType);
    };
  }
}
