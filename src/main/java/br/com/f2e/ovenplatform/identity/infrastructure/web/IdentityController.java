package br.com.f2e.ovenplatform.identity.infrastructure.web;

import br.com.f2e.ovenplatform.identity.application.IdentityService;
import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.UserRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.UserResponse;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class IdentityController {

  private final IdentityService identityService;

  public IdentityController(IdentityService identityService) {
    this.identityService = identityService;
  }

  @PostMapping(version = "1.0")
  public ResponseEntity<UserResponse> createUser(
      @CurrentTenantId UUID tenantId, @Valid @RequestBody UserRequest userRequest) {

    var userResponse =
        UserResponse.fromEntity(
            identityService.create(
                tenantId, userRequest.email(), userRequest.password(), userRequest.role()));
    var uri = ResourceUriBuilder.buildLocation(userResponse.id());

    return ResponseEntity.created(uri).body(userResponse);
  }

  @GetMapping(value = "/{id}", version = "1.0")
  public ResponseEntity<UserResponse> findByIdAndTenantId(
      @CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    return ResponseEntity.ok(
        UserResponse.fromEntity(identityService.findByIdAndTenantId(id, tenantId)));
  }
}
