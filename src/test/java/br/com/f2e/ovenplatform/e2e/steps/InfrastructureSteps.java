package br.com.f2e.ovenplatform.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

@ScenarioScope
public class InfrastructureSteps {

  private final JdbcTemplate jdbcTemplate;
  private final ConfigurableApplicationContext applicationContext;
  private final int port;

  public InfrastructureSteps(
      JdbcTemplate jdbcTemplate,
      ConfigurableApplicationContext applicationContext,
      @Value("${local.server.port}") int port) {
    this.jdbcTemplate = jdbcTemplate;
    this.applicationContext = applicationContext;
    this.port = port;
  }

  @Then("the API should be running")
  public void theApiShouldBeRunning() {
    assertThat(applicationContext.isActive()).isTrue();
    assertThat(port).isPositive();
  }

  @Then("the database should be available")
  public void theDatabaseShouldBeAvailable() {
    Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
    assertThat(result).isEqualTo(1);
  }

  @And("the Liquibase schema should be applied")
  public void theLiquibaseSchemaShouldBeApplied() {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables where table_name = 'tenants'",
            Integer.class);

    assertThat(count).isEqualTo(1);
  }
}
