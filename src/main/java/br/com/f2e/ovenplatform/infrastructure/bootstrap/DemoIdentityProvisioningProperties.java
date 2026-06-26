package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oven.demo.identity")
public record DemoIdentityProvisioningProperties(
    String tenantName, String userEmail, String userPassword) {}
