package br.com.f2e.ovenplatform.media.infrastructure.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.media.application.storage.StoredObjectNotFoundException;
import br.com.f2e.ovenplatform.media.application.storage.UploadAuthorizationSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageAdapterTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID IMAGE_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final String CONTENT_TYPE = "image/webp";
  private static final long SIZE_BYTES = 42_000L;
  private static final String CHECKSUM = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";
  private static final String OBJECT_KEY =
      "tenants/%s/images/%s.webp".formatted(TENANT_ID, IMAGE_ID);
  private static final Duration DURATION = Duration.ofMinutes(5);
  private static final String BUCKET = "oven-platform-images";

  @Mock private S3Client s3Client;
  @Captor private ArgumentCaptor<Consumer<HeadObjectRequest.Builder>> headObjectRequestCaptor;
  @Captor private ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> deleteObjectRequestCaptor;

  private AwsMediaProperties awsMediaProperties;
  private S3Presigner s3Presigner;
  private S3ImageStorageAdapter s3ImageStorageAdapter;

  @BeforeEach
  void setUp() {
    awsMediaProperties = new AwsMediaProperties(BUCKET, Region.US_EAST_1.id(), DURATION);
    s3Presigner =
        S3Presigner.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key")))
            .build();
    s3ImageStorageAdapter = new S3ImageStorageAdapter(s3Client, s3Presigner, awsMediaProperties);
  }

  @AfterEach
  void tearDown() {
    s3Presigner.close();
  }

  @Test
  void shouldAuthorizeUploadWithConfiguredBucketAndRequestedMetadata() {
    var uploadAuthorization =
        new UploadAuthorizationSpec(OBJECT_KEY, CONTENT_TYPE, SIZE_BYTES, CHECKSUM);
    var beforeAuthorization = Instant.now();

    var authorization = s3ImageStorageAdapter.authorizeUpload(uploadAuthorization);

    assertThat(authorization.uploadUrl().getHost()).isEqualTo(BUCKET + ".s3.amazonaws.com");
    assertThat(authorization.uploadUrl().getPath()).isEqualTo("/" + OBJECT_KEY);
    assertThat(authorization.method()).isEqualTo("PUT");
    assertThat(authorization.expiresAt())
        .isBetween(beforeAuthorization.plus(DURATION), Instant.now().plus(DURATION));
    assertThat(authorization.requiredHeaders())
        .containsEntry("x-amz-checksum-sha256", CHECKSUM)
        .containsEntry("content-type", CONTENT_TYPE)
        .doesNotContainKeys("host", "content-length");
  }

  @Test
  void shouldRetrieveAndMapObjectMetadata() {
    var response =
        HeadObjectResponse.builder()
            .contentType(CONTENT_TYPE)
            .contentLength(SIZE_BYTES)
            .checksumSHA256(CHECKSUM)
            .build();
    when(s3Client.headObject(headObjectRequestCaptor.capture())).thenReturn(response);

    var metadata = s3ImageStorageAdapter.getMetadata(OBJECT_KEY);

    var request = buildHeadObjectRequest(headObjectRequestCaptor.getValue());
    assertThat(request.bucket()).isEqualTo(BUCKET);
    assertThat(request.key()).isEqualTo(OBJECT_KEY);
    assertThat(request.checksumMode()).isEqualTo(ChecksumMode.ENABLED);
    assertThat(metadata.contentType()).isEqualTo(CONTENT_TYPE);
    assertThat(metadata.sizeBytes()).isEqualTo(SIZE_BYTES);
    assertThat(metadata.checksum()).isEqualTo(CHECKSUM);
  }

  @Test
  void shouldTranslateMissingObjectToProviderNeutralException() {
    when(s3Client.headObject(headObjectRequestCaptor.capture()))
        .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

    assertThatThrownBy(() -> s3ImageStorageAdapter.getMetadata(OBJECT_KEY))
        .isInstanceOf(StoredObjectNotFoundException.class)
        .hasMessage("Stored object not found: " + OBJECT_KEY);
  }

  @Test
  void shouldPreserveUnexpectedS3Exception() {
    var exception = S3Exception.builder().statusCode(403).message("Forbidden").build();
    when(s3Client.headObject(headObjectRequestCaptor.capture())).thenThrow(exception);

    assertThatThrownBy(() -> s3ImageStorageAdapter.getMetadata(OBJECT_KEY)).isSameAs(exception);
  }

  @Test
  void shouldDeleteObjectFromConfiguredBucket() {
    s3ImageStorageAdapter.delete(OBJECT_KEY);

    verify(s3Client).deleteObject(deleteObjectRequestCaptor.capture());
    var request = buildDeleteObjectRequest(deleteObjectRequestCaptor.getValue());
    assertThat(request.bucket()).isEqualTo(BUCKET);
    assertThat(request.key()).isEqualTo(OBJECT_KEY);
  }

  private HeadObjectRequest buildHeadObjectRequest(
      Consumer<HeadObjectRequest.Builder> requestCustomizer) {
    var builder = HeadObjectRequest.builder();
    requestCustomizer.accept(builder);
    return builder.build();
  }

  private DeleteObjectRequest buildDeleteObjectRequest(
      Consumer<DeleteObjectRequest.Builder> requestCustomizer) {
    var builder = DeleteObjectRequest.builder();
    requestCustomizer.accept(builder);
    return builder.build();
  }
}
