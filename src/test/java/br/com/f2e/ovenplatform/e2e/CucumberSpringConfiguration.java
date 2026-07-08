package br.com.f2e.ovenplatform.e2e;

import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SuppressWarnings("unused")
@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
      "oven.kafka.topics.auto-create=false",
      "spring.kafka.listener.auto-startup=false"
    })
@Import(PostgresTestContainerConfiguration.class)
class CucumberSpringConfiguration {}
