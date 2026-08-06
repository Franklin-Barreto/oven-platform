package br.com.f2e.ovenplatform.payment.infrastructure.stripe;

import com.stripe.StripeClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfiguration {

  @Bean
  StripeClient stripeClient(StripeProperties stripeProperties) {
    return new StripeClient(stripeProperties.secretKey());
  }
}
