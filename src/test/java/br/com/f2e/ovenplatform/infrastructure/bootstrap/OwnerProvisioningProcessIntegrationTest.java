package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.OvenPlatformApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class OwnerProvisioningProcessIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

  @TempDir Path tempDirectory;

  @Test
  void shouldProvisionOwnerAndExitSuccessfully() throws Exception {
    try (var postgres = new PostgreSQLContainer(POSTGRES_IMAGE)) {
      postgres.start();

      var outputFile = tempDirectory.resolve("bootstrap-owner.log");
      var process =
          new ProcessBuilder(
                  javaExecutable(),
                  "-cp",
                  testClasspath(),
                  OvenPlatformApplication.class.getName(),
                  "--spring.profiles.active=bootstrap-owner",
                  "--spring.docker.compose.enabled=false",
                  "--spring.datasource.url=" + postgres.getJdbcUrl(),
                  "--spring.datasource.username=" + postgres.getUsername(),
                  "--spring.datasource.password=" + postgres.getPassword(),
                  "--jwt.secret=" + testJwtSecret(),
                  "--oven.bootstrap.owner.tenant-name=Don Corleone Pizzeria",
                  "--oven.bootstrap.owner.email=owner@oven.local",
                  "--oven.bootstrap.owner.password=OwnerPass123!")
              .redirectErrorStream(true)
              .redirectOutput(outputFile.toFile())
              .start();

      var finished = process.waitFor(60, SECONDS);
      if (!finished) {
        process.destroyForcibly();
      }

      var output = Files.readString(outputFile);
      assertThat(finished).as(output).isTrue();
      assertThat(process.exitValue()).as(output).isZero();
      assertThat(output).contains("outcome=PROVISIONED");
    }
  }

  private static String javaExecutable() {
    return Path.of(System.getProperty("java.home"), "bin", "java").toString();
  }

  private static String testClasspath() {
    return System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
  }

  private static String testJwtSecret() {
    var clearlyNonSecretTestValue = "test-jwt-signing-key-".repeat(4);
    return Base64.getEncoder().encodeToString(clearlyNonSecretTestValue.getBytes(UTF_8));
  }
}
