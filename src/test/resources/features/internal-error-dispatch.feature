Feature: Preserve authenticated server errors

  Background:
    Given an OWNER user exists for tenant "Don Corleone Pizzeria"
    And I am authenticated as that user

  Scenario: An internal failure is not represented as an authentication failure
    When I request a protected endpoint that fails unexpectedly
    Then the response should be an internal server error
    And the same access token should remain valid
