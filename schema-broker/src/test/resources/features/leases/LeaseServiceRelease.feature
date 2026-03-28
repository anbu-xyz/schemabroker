Feature: Release lease

  Background:
    Given the database is empty

  Scenario: Release an active lease
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
    And there is an active lease for schema "SCHEMA_01" owned by "client-1"
    When I release that lease
    Then the released lease status is "RELEASED"

  Scenario: Release is idempotent for an already released lease
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
    And there is a released lease for schema "SCHEMA_01" owned by "client-1"
    When I release that lease
    Then the released lease status is "RELEASED"

  Scenario: Release for a non-existent lease returns nothing
    When I release a random lease id
    Then no released lease is returned

