package br.com.f2e.ovenplatform.media.application.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AvailableImageLookup {

  AvailableImage getAvailableImage(UUID tenantId, UUID imageId);

  List<AvailableImage> getAvailableImages(UUID tenantId, Set<UUID> imageIds);
}
