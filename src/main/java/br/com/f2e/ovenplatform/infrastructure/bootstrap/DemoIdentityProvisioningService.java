package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import br.com.f2e.ovenplatform.identity.application.TenantMembershipRepository;
import br.com.f2e.ovenplatform.identity.application.UserRepository;
import br.com.f2e.ovenplatform.identity.domain.TenantMembership;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.identity.domain.User;
import br.com.f2e.ovenplatform.tenant.application.TenantRepository;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local-demo")
@EnableConfigurationProperties(DemoIdentityProvisioningProperties.class)
public class DemoIdentityProvisioningService {

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final TenantMembershipRepository tenantMembershipRepository;
  private final PasswordEncoder passwordEncoder;
  private final DemoIdentityProvisioningProperties properties;

  public DemoIdentityProvisioningService(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      TenantMembershipRepository tenantMembershipRepository,
      PasswordEncoder passwordEncoder,
      DemoIdentityProvisioningProperties properties) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.tenantMembershipRepository = tenantMembershipRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
  }

  @Transactional
  public void provision() {
    var tenant = ensureTenant();
    var user = ensureUser();

    ensureMembership(user, tenant);
  }

  private Tenant ensureTenant() {
    return tenantRepository
        .findByName(properties.tenantName())
        .orElseGet(() -> tenantRepository.save(new Tenant(properties.tenantName(), Plan.MVP)));
  }

  private User ensureUser() {
    return userRepository
        .findByEmail(properties.userEmail())
        .orElseGet(
            () ->
                userRepository.save(
                    new User(
                        properties.userEmail(),
                        passwordEncoder.encode(properties.userPassword()))));
  }

  private void ensureMembership(User user, Tenant tenant) {
    if (tenantMembershipRepository
        .findByUserIdAndTenantId(user.getId(), tenant.getId())
        .isPresent()) {
      return;
    }

    tenantMembershipRepository.save(
        new TenantMembership(user, tenant.getId(), TenantMembershipRole.OWNER));
  }
}
