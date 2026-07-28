package br.com.f2e.ovenplatform.media.infrastructure.aws;

import br.com.f2e.ovenplatform.media.application.storage.ImageStorage;
import br.com.f2e.ovenplatform.media.application.storage.ImageUploadAuthorization;
import br.com.f2e.ovenplatform.media.application.storage.StoredObjectMetadata;
import br.com.f2e.ovenplatform.media.application.storage.StoredObjectNotFoundException;
import br.com.f2e.ovenplatform.media.application.storage.UploadAuthorizationSpec;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Component
public class S3ImageStorageAdapter implements ImageStorage {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final AwsMediaProperties properties;

  public S3ImageStorageAdapter(
      S3Client s3Client, S3Presigner s3Presigner, AwsMediaProperties properties) {

    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.properties = properties;
  }

  @Override
  public ImageUploadAuthorization authorizeUpload(UploadAuthorizationSpec spec) {
    var presignedRequest =
        s3Presigner.presignPutObject(
            presign ->
                presign
                    .signatureDuration(properties.uploadAuthorizationTtl())
                    .putObjectRequest(
                        put ->
                            put.bucket(properties.bucket())
                                .key(spec.objectKey())
                                .contentType(spec.contentType())
                                .contentLength(spec.sizeBytes())
                                .checksumSHA256(spec.checksum())));

    return new ImageUploadAuthorization(
        URI.create(presignedRequest.url().toString()),
        presignedRequest.httpRequest().method().name(),
        requiredHeaders(presignedRequest),
        presignedRequest.expiration());
  }

  @Override
  public StoredObjectMetadata getMetadata(String objectKey) {
    try {
      var response =
          s3Client.headObject(
              request ->
                  request
                      .bucket(properties.bucket())
                      .key(objectKey)
                      .checksumMode(ChecksumMode.ENABLED));

      return new StoredObjectMetadata(
          response.contentType(), response.contentLength(), response.checksumSHA256());
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        throw new StoredObjectNotFoundException(objectKey);
      }

      throw exception;
    }
  }

  @Override
  public void delete(String objectKey) {
    s3Client.deleteObject(request -> request.bucket(properties.bucket()).key(objectKey));
  }

  private Map<String, String> requiredHeaders(PresignedPutObjectRequest presignedRequest) {

    return presignedRequest.signedHeaders().entrySet().stream()
        .filter(entry -> isClientControlledHeader(entry.getKey()))
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> String.join(",", entry.getValue())));
  }

  private boolean isClientControlledHeader(String headerName) {
    return !headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length");
  }
}
