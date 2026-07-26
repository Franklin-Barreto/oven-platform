package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import static br.com.f2e.ovenplatform.identity.domain.validation.EmailNormalizer.normalize;
import static br.com.f2e.ovenplatform.infrastructure.bootstrap.OwnerProvisioningResult.Outcome.ALREADY_PROVISIONED;
import static br.com.f2e.ovenplatform.infrastructure.bootstrap.OwnerProvisioningResult.Outcome.PROVISIONED;

import br.com.f2e.ovenplatform.identity.application.port.TenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.application.port.UserRepository;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipStatus;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.tenant.application.TenantRepository;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Status;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerProvisioningService {

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final TenantMembershipRepository tenantMembershipRepository;
  private final PasswordEncoder passwordEncoder;

  public OwnerProvisioningService(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      TenantMembershipRepository tenantMembershipRepository,
      PasswordEncoder passwordEncoder) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public OwnerProvisioningResult provision(OwnerProvisioningCommand command) {
    var normalizedEmail = normalize(command.email());
    var tenant = findOrCreateTenant(command.tenantName());
    var user = findOrCreateUser(normalizedEmail, command.password());
    var existingMembership =
        tenantMembershipRepository.findByUserIdAndTenantId(user.getId(), tenant.getId());

    if (existingMembership.isPresent()) {
      ensureOwnerMembershipIsValid(existingMembership.orElseThrow());
      return new OwnerProvisioningResult(tenant.getId(), user.getId(), ALREADY_PROVISIONED);
    }

    tenantMembershipRepository.save(TenantMembership.owner(user, tenant.getId()));

    return new OwnerProvisioningResult(tenant.getId(), user.getId(), PROVISIONED);
  }

  private Tenant findOrCreateTenant(String tenantName) {
    var tenant =
        tenantRepository
            .findByName(tenantName)
            .orElseGet(() -> tenantRepository.save(new Tenant(tenantName, Plan.MVP)));

    if (tenant.getStatus() != Status.ACTIVE) {
      throw new OwnerProvisioningConflictException("Existing tenant is not active");
    }

    return tenant;
  }

  private User findOrCreateUser(String email, String rawPassword) {
    var existingUser = userRepository.findByEmail(email);

    if (existingUser.isEmpty()) {
      return userRepository.save(new User(email, passwordEncoder.encode(rawPassword)));
    }

    var user = existingUser.orElseThrow();

    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw new OwnerProvisioningConflictException(
          "Existing user credentials do not match the bootstrap secret");
    }

    return user;
  }

  private void ensureOwnerMembershipIsValid(TenantMembership membership) {
    if (!membership.getRoles().contains(TenantMembershipRole.OWNER)) {
      throw new OwnerProvisioningConflictException("Existing membership does not have OWNER role");
    }

    if (membership.getStatus() != TenantMembershipStatus.ACTIVE) {
      throw new OwnerProvisioningConflictException("Existing OWNER membership is not active");
    }
  }
}
