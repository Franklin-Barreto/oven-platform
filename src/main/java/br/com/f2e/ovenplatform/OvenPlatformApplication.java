/* (C)2026 */
package br.com.f2e.ovenplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Profiles;

@SpringBootApplication
public class OvenPlatformApplication {

  public static void main(String[] args) {
    var context = SpringApplication.run(OvenPlatformApplication.class, args);
    exitAfterCommandMode(context);
  }

  static void exitAfterCommandMode(ConfigurableApplicationContext context) {
    if (context.getEnvironment().acceptsProfiles(Profiles.of("bootstrap-owner"))) {
      System.exit(SpringApplication.exit(context));
    }
  }
}
