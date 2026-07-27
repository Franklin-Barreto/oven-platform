package br.com.f2e.ovenplatform.media.application.storage;

public interface ImageStorage {

  ImageUploadAuthorization authorizeUpload(UploadAuthorizationSpec spec);

  StoredObjectMetadata getMetadata(String objectKey);

  ImageReadAuthorization authorizeRead(String objectKey);

  void delete(String objectKey);
}
