Feature: API E2E infrastructure

  Scenario: Cucumber, Spring and PostgreSQL Testcontainer are available
    Then the API should be running
    And the database should be available
    And the Liquibase schema should be applied