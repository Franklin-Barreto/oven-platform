package br.com.f2e.ovenplatform.identity.infrastructure.security.config;

import br.com.f2e.ovenplatform.identity.infrastructure.security.web.CurrentTenantIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CurrentTenantWebMvcConfig implements WebMvcConfigurer {

  private final CurrentTenantIdArgumentResolver currentTenantIdArgumentResolver;

  public CurrentTenantWebMvcConfig(
      CurrentTenantIdArgumentResolver currentTenantIdArgumentResolver) {
    this.currentTenantIdArgumentResolver = currentTenantIdArgumentResolver;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentTenantIdArgumentResolver);
  }
}
