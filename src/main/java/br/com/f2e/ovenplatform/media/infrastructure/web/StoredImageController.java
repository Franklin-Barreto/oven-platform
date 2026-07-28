package br.com.f2e.ovenplatform.media.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.media.application.PrepareImageUploadCommand;
import br.com.f2e.ovenplatform.media.application.StoredImageService;
import br.com.f2e.ovenplatform.media.infrastructure.web.dto.PrepareImageUploadRequest;
import br.com.f2e.ovenplatform.media.infrastructure.web.dto.PreparedImageUploadResponse;
import br.com.f2e.ovenplatform.media.infrastructure.web.dto.PublicImageLocationResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
public class StoredImageController {

  private final StoredImageService imageService;

  public StoredImageController(StoredImageService imageService) {
    this.imageService = imageService;
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PostMapping(path = "/uploads", version = API_VERSION_VALUE)
  public ResponseEntity<PreparedImageUploadResponse> prepareUpload(
      @CurrentTenantId UUID tenantId, @RequestBody @Valid PrepareImageUploadRequest request) {

    var preparedUpload =
        imageService.prepareUpload(
            tenantId,
            new PrepareImageUploadCommand(
                request.contentType(), request.sizeBytes(), request.checksum()));

    var response = PreparedImageUploadResponse.from(preparedUpload);
    var location = ResourceUriBuilder.buildLocation(response.imageId());

    return ResponseEntity.created(location).body(response);
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @PutMapping(path = "/{imageId}/confirmation", version = API_VERSION_VALUE)
  public ResponseEntity<Void> confirmUpload(
      @CurrentTenantId UUID tenantId, @PathVariable UUID imageId) {
    imageService.confirmUpload(tenantId, imageId);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('CATALOG_READ')")
  @GetMapping(path = "/{imageId}/location", version = API_VERSION_VALUE)
  public ResponseEntity<PublicImageLocationResponse> resolvePublicLocation(
      @CurrentTenantId UUID tenantId, @PathVariable UUID imageId) {

    var location = imageService.resolvePublicLocation(tenantId, imageId);

    return ResponseEntity.ok(PublicImageLocationResponse.from(location));
  }

  @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
  @DeleteMapping(path = "/{imageId}", version = API_VERSION_VALUE)
  public ResponseEntity<Void> delete(@CurrentTenantId UUID tenantId, @PathVariable UUID imageId) {

    imageService.delete(tenantId, imageId);

    return ResponseEntity.noContent().build();
  }
}
