package br.com.f2e.ovenplatform.identity.infrastructure.security.config;

import br.com.f2e.ovenplatform.identity.infrastructure.security.tenant.TenantAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantAccessWebMvcConfig implements WebMvcConfigurer {

  private final TenantAccessInterceptor tenantAccessInterceptor;

  public TenantAccessWebMvcConfig(TenantAccessInterceptor tenantAccessInterceptor) {
    this.tenantAccessInterceptor = tenantAccessInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(tenantAccessInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/auth/login", "/actuator/**");
  }
}
