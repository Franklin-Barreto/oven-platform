package br.com.f2e.ovenplatform.identity.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;

import br.com.f2e.ovenplatform.identity.application.IdentityService;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.UserRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/users")
public class IdentityController {

  private final IdentityService identityService;

  public IdentityController(IdentityService identityService) {
    this.identityService = identityService;
  }

  @PostMapping(version = "1.0")
  public ResponseEntity<UserResponse> createUser(
      @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
      @Valid @RequestBody UserRequest userRequest,
      HttpServletRequest request) {
    var userResponse =
        UserResponse.fromEntity(
            identityService.create(
                tenantId, userRequest.email(), userRequest.password(), userRequest.role()));
    var uri =
        UriComponentsBuilder.fromPath(request.getRequestURI() + "/{id}")
            .buildAndExpand(userResponse.id())
            .toUri();
    return ResponseEntity.created(uri).body(userResponse);
  }

  @GetMapping(value = "/{id}", version = "1.0")
  public ResponseEntity<UserResponse> findByIdAndTenantId(
      @RequestHeader(TENANT_ID_HEADER) UUID tenantId, @PathVariable UUID id) {
    return ResponseEntity.ok(
        UserResponse.fromEntity(identityService.findByIdAndTenantId(id, tenantId)));
  }
}
