Feature: Endpoint authorization

  Scenario: Combined attendant and kitchen user accesses both operation areas
    Given an ATTENDANT and KITCHEN user exists for tenant "Combined Roles Pizzeria"
    And I am authenticated as that user
    When I list the tenant orders
    Then the request should be allowed
    When I list the kitchen tickets
    Then the request should be allowed
