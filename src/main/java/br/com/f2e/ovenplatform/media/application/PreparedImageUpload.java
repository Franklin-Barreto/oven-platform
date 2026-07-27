package br.com.f2e.ovenplatform.media.application;

import br.com.f2e.ovenplatform.media.application.storage.ImageUploadAuthorization;
import java.util.UUID;

public record PreparedImageUpload(UUID imageId, ImageUploadAuthorization authorization) {}
