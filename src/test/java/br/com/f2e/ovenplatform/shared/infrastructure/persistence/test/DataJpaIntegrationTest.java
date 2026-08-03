package br.com.f2e.ovenplatform.shared.infrastructure.persistence.test;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestContainerConfiguration.class)
@EnableJpaAuditing
@Execution(ExecutionMode.SAME_THREAD)
public abstract class DataJpaIntegrationTest {

  @Autowired protected EntityManager entityManager;

  protected void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
