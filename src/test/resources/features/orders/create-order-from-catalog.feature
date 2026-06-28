Feature: Create order from catalog

  Background:
    Given an OWNER user exists for tenant "Don Corleone Pizzeria"
    And I am authenticated as that user

  Scenario: Create an order from an existing catalog product
    Given a category named "Pizzas" exists
    And a product named "Pizza Portuguesa" priced at 35.40 exists in category "Pizzas"
    When I create an order with 2 units of product "Pizza Portuguesa"
    Then the order should be created successfully
    And the order status should be "CREATED"
    And the order total should be 70.80
    And the order item should contain product "Pizza Portuguesa"
    And the order item unit price should be 35.40