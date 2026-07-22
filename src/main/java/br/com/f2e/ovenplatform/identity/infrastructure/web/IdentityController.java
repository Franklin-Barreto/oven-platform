package br.com.f2e.ovenplatform.identity.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.identity.application.team.CreateTenantUserCommand;
import br.com.f2e.ovenplatform.identity.application.team.DeactivateTenantMembershipCommand;
import br.com.f2e.ovenplatform.identity.application.team.ReactivateTenantMembershipCommand;
import br.com.f2e.ovenplatform.identity.application.team.ReplaceTenantMembershipRolesCommand;
import br.com.f2e.ovenplatform.identity.application.team.TenantTeamService;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.infrastructure.security.dto.AuthenticatedUser;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.ChangeTenantMembershipStatusRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.ReplaceTenantMembershipRolesRequest;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.TenantUserResponse;
import br.com.f2e.ovenplatform.identity.infrastructure.web.dto.user.UserRequest;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class IdentityController {

  private final TenantTeamService tenantTeamService;

  public IdentityController(TenantTeamService tenantTeamService) {
    this.tenantTeamService = tenantTeamService;
  }

  @PreAuthorize("hasAuthority('TEAM_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<TenantUserResponse> createUser(
      @CurrentTenantId UUID tenantId,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody UserRequest userRequest) {

    var userResponse =
        TenantUserResponse.from(
            tenantTeamService.createTenantUser(
                new CreateTenantUserCommand(
                    tenantId,
                    authenticatedUser.userId(),
                    userRequest.email(),
                    userRequest.password(),
                    userRequest.roles())));

    var uri = ResourceUriBuilder.buildLocation(userResponse.id());

    return ResponseEntity.created(uri).body(userResponse);
  }

  @PreAuthorize("hasAuthority('TEAM_READ')")
  @GetMapping(value = "/{id}", version = API_VERSION_VALUE)
  public ResponseEntity<TenantUserResponse> findByIdAndTenantId(
      @CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    return ResponseEntity.ok(
        TenantUserResponse.from(tenantTeamService.findTenantUserById(tenantId, id)));
  }

  @PreAuthorize("hasAuthority('TEAM_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<TenantUserResponse>> list(@CurrentTenantId UUID tenantId) {
    return ResponseEntity.ok(
        tenantTeamService.listTenantMemberships(tenantId).stream()
            .map(TenantUserResponse::from)
            .toList());
  }

  @PreAuthorize("hasAuthority('TEAM_MANAGE')")
  @PutMapping(value = "/{targetUserId}/status", version = API_VERSION_VALUE)
  public ResponseEntity<Void> changeStatus(
      @CurrentTenantId UUID tenantId,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID targetUserId,
      @Valid @RequestBody ChangeTenantMembershipStatusRequest request) {

    if (request.status() == TenantMembershipStatus.ACTIVE) {
      tenantTeamService.reactivateTenantMembership(
          new ReactivateTenantMembershipCommand(
              tenantId, authenticatedUser.userId(), targetUserId));
    } else {
      tenantTeamService.deactivateTenantMembership(
          new DeactivateTenantMembershipCommand(
              tenantId, authenticatedUser.userId(), targetUserId));
    }

    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('TEAM_MANAGE')")
  @PutMapping(value = "/{targetUserId}/roles", version = API_VERSION_VALUE)
  public ResponseEntity<Void> replaceRoles(
      @CurrentTenantId UUID tenantId,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @PathVariable UUID targetUserId,
      @Valid @RequestBody ReplaceTenantMembershipRolesRequest request) {

    tenantTeamService.replaceTenantMembershipRoles(
        new ReplaceTenantMembershipRolesCommand(
            tenantId, authenticatedUser.userId(), targetUserId, request.roles()));

    return ResponseEntity.noContent().build();
  }
}
