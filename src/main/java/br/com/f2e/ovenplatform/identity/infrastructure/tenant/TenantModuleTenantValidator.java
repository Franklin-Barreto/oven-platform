package br.com.f2e.ovenplatform.identity.infrastructure.tenant;

import br.com.f2e.ovenplatform.identity.application.port.TenantValidator;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.tenant.application.api.TenantLookup;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TenantModuleTenantValidator implements TenantValidator {

  public static final String RESOURCE = "Tenant";

  private final TenantLookup tenantLookup;

  public TenantModuleTenantValidator(TenantLookup tenantLookup) {
    this.tenantLookup = tenantLookup;
  }

  @Override
  public void ensureTenantExists(UUID tenantId) {
    if (!tenantLookup.existsById(tenantId)) {
      throw new ResourceNotFoundException(RESOURCE, tenantId);
    }
  }
}
