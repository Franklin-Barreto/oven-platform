package br.com.f2e.ovenplatform.identity.infrastructure.tenant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.tenant.application.api.TenantLookup;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantModuleTenantValidatorTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");

  @Mock private TenantLookup tenantLookup;

  @Test
  void shouldPassWhenTenantExists() {
    when(tenantLookup.existsById(TENANT_ID)).thenReturn(true);

    var validator = new TenantModuleTenantValidator(tenantLookup);

    validator.ensureTenantExists(TENANT_ID);

    verify(tenantLookup).existsById(TENANT_ID);
  }

  @Test
  void shouldThrowResourceNotFoundWhenTenantDoesNotExist() {
    when(tenantLookup.existsById(TENANT_ID)).thenReturn(false);

    var validator = new TenantModuleTenantValidator(tenantLookup);

    assertThatThrownBy(() -> validator.ensureTenantExists(TENANT_ID))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Tenant id: a6210129-f1d5-4942-8d0a-b144e518aecc not found");

    verify(tenantLookup).existsById(TENANT_ID);
  }
}
